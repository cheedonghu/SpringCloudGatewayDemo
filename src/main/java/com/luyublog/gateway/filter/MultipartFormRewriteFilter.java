package com.luyublog.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 另一种实现表单编辑方法
 * 直接操作字节数组，根据表单的boundary将字节数组分割为多部分，保留二进制不处理，仅处理要用的String部分
 */
//@Component
public class MultipartFormRewriteFilter implements GlobalFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerRequest serverRequest =
                ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());


        // 读取 multipart/form-data 为 MultiValueMap<String, Part>
        Mono<Map<String, List<Part>>> multipartMono = serverRequest
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Part>>>() {
                });

        return multipartMono.flatMap(partsMap -> {
            Map<String, String> simpleFields = new HashMap<>();

            // 收集普通表单字段
            for (Map.Entry<String, List<Part>> entry : partsMap.entrySet()) {
                for (Part part : entry.getValue()) {
                    if (isFormField(part)) {
                        String value = partToString(part);
                        simpleFields.put(entry.getKey(), value);
                    }
                }
            }

            try {
                // 转换为 JSON
                String jsonString = objectMapper.writeValueAsString(simpleFields);

                // 新建一个 application/json 类型的 Part
                JsonPart jsonPart = new JsonPart("json", jsonString);

                // 构造新的 parts，保留原始文件字段 + 新增 json 字段
                MultiValueMap<String, List<Part>> newParts = new LinkedMultiValueMap<>();
                List<Part> objects = new ArrayList<>();
                objects.add(jsonPart);
                newParts.add("json", objects);

                BodyInserters.MultipartInserter bodyInserter =
                        BodyInserters.fromMultipartData(newParts);

                HttpHeaders headers = new HttpHeaders();
                headers.putAll(exchange.getRequest().getHeaders());
                headers.remove(HttpHeaders.CONTENT_LENGTH);

//                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
                // 创建输出消息用于接收转换后的数据
                ServerHttpResponse response = exchange.getResponse();
                DataBufferFactory bufferFactory = response.bufferFactory();
                DefaultClientHttpRequest clientHttpRequest = new DefaultClientHttpRequest(
                        HttpMethod.POST,
                        serverRequest.uri(),
                        bufferFactory
                );

                return bodyInserter.insert(clientHttpRequest, new BodyInserterContext())
                        .then(Mono.defer(() -> {
                            ServerHttpRequest decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return clientHttpRequest.getBody();
                                }

                                @Override
                                public HttpHeaders getHeaders() {
                                    HttpHeaders httpHeaders = new HttpHeaders();
                                    httpHeaders.putAll(headers);
                                    httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                                    return httpHeaders;
                                }
                            };
                            return chain.filter(exchange.mutate().request(decorator).build());
                        }));
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    private boolean isFormField(Part part) {
        return part.headers().getContentType() == null ||
                part.headers().getContentType().toString().startsWith("text");
    }

    private String partToString(Part part) {
        return part.content()
                .reduce((buf1, buf2) -> {
                    buf1.write(buf2);
                    return buf1;
                })
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .block();
    }

    /**
     * 自定义 JSON 类型的 Part
     */
    static class JsonPart implements Part {
        private final String name;
        private final String json;

        JsonPart(String name, String json) {
            this.name = name;
            this.json = json;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HttpHeaders headers() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return headers;
        }

        @Override
        public Flux<DataBuffer> content() {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return Flux.just(new DefaultDataBufferFactory().wrap(bytes));
        }
    }


    // 简化的ClientHttpRequest实现
    class DefaultClientHttpRequest implements ClientHttpRequest {
        private final HttpMethod method;
        private final URI uri;
        private final HttpHeaders headers;
        private final DataBufferFactory bufferFactory;
        private Flux<DataBuffer> body = Flux.empty();

        public DefaultClientHttpRequest(HttpMethod method, URI uri, DataBufferFactory bufferFactory) {
            this.method = method;
            this.uri = uri;
            this.headers = new HttpHeaders();
            this.bufferFactory = bufferFactory;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public MultiValueMap<String, HttpCookie> getCookies() {
            return new LinkedMultiValueMap<>();
        }

        @Override
        public <T> T getNativeRequest() {
            return null;
        }

        @Override
        public DataBufferFactory bufferFactory() {
            return bufferFactory;
        }

        @Override
        public void beforeCommit(Supplier<? extends Mono<Void>> action) {
            // 空实现
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            this.body = Flux.from(body);
            return Mono.empty();
        }

        @Override
        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return writeWith(Flux.from(body).flatMap(Flux::from));
        }

        @Override
        public Mono<Void> setComplete() {
            return Mono.empty();
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        public Flux<DataBuffer> getBody() {
            return body;
        }
    }
}


