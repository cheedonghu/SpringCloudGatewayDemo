//package com.luyublog.gateway.filter;
//
//import cn.hutool.core.io.resource.MultiResource;
//import cn.hutool.core.io.resource.Resource;
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.http.ContentType;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.HttpMessageReader;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
//import org.springframework.web.reactive.function.BodyInserter;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.springframework.web.reactive.function.server.HandlerStrategies;
//import org.springframework.web.reactive.function.server.ServerRequest;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
///**
// * @description:
// *
// * 参考：https://blog.csdn.net/qq_36966137/article/details/128536391
// * 方法：ModifyRequestBodyGatewayFilterFactory
// * @author: east
// * @date: 2023/9/14
// */
//@Slf4j
//public class AuthorizeFilter implements GlobalFilter, Ordered {
//
//    private static final String CONTENT_DISPOSITION_TEMPLATE = "Content-Disposition: form-data; name=\"{}\"\r\n\r\n";
//    private static final String CONTENT_DISPOSITION_FILE_TEMPLATE = "Content-Disposition: form-data; name=\"{}\"; filename=\"{}\"\r\n";
//
//    private static final String CONTENT_TYPE_MULTIPART_PREFIX = ContentType.MULTIPART.getValue() + "; boundary=";
//    private static final String CONTENT_TYPE_FILE_TEMPLATE = "Content-Type: {}\r\n\r\n";
//
//    private final static String TOKEN_USERINFO_CACHE_FLAG = "TI-%s";
//    private static final List<HttpMessageReader<?>> MESSAGE_READERS = HandlerStrategies.withDefaults().messageReaders();
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)   {
//        Mono<Void> mono = chain.filter(exchange);
//        ServerHttpRequest request = exchange.getRequest();
//        MediaType contentType = request.getHeaders().getContentType();
//
//        if (Objects.nonNull(contentType) && Objects.nonNull(exchange.getRequest().getMethod())
//                && exchange.getRequest().getMethod().equals(HttpMethod.POST)) {
//            if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
//                // json 请求体处理
//                mono = this.transferBody(exchange, chain);
//            }else if(MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)){
//                // multipart/form-data处理
//                mono = this.fileRequest(contentType,exchange,chain);
//            }
////            else if(MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)){
////                // x-www-form-urlencoded 格式处理
////                mono = this.xwFromBody(exchange,chain);
////            }
//        }else{
//            if(exchange.getRequest().getMethod().equals(HttpMethod.GET)){
//                Map<String, String> queryParams = exchange.getRequest().getQueryParams().toSingleValueMap();
//                log.info("queryParams:{}", queryParams);
//            }
//        }
//        return mono;
//    }
//
//    /**
//     * 修改修改body参数
//     */
//    private Mono<Void> transferBody(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
//        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(Mono::just);
//
//        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
//        HttpHeaders headers = new HttpHeaders();
//        headers.putAll(exchange.getRequest().getHeaders());
//        headers.remove(HttpHeaders.CONTENT_LENGTH);
//        MyCachedBodyOutputMessage outputMessage = new MyCachedBodyOutputMessage(exchange, headers);
//        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
//        requestBuilder.headers(k -> k.remove("Content-length"));
//
//        Mono mono = bodyInserter.insert(outputMessage, new BodyInserterContext())
//                .then(Mono.defer(() -> {
//                    //解决body内数据过长读取不完整的问题
//                    Flux<DataBuffer> body = outputMessage.getBody();
//                    DataBufferHolder holder = new DataBufferHolder();
//                    try{
//                        body.subscribe(dataBuffer -> {
//                            int len = dataBuffer.readableByteCount();
//                            holder.length = len;
//                            byte[] bytes = new byte[len];
//                            dataBuffer.read(bytes);
//                            DataBufferUtils.release(dataBuffer);
//                            String oldBody = new String(bytes, StandardCharsets.UTF_8);
//                            JsonNode jsonNode = objectMapper.readTree(in);
//                            //到这可以读取数据，做校验之类的
//                            //JsonNode token = oldDataJSON.get("token");
//                            jsonNode .set("test","修改数据");
//                            DataBuffer data = outputMessage.bufferFactory().allocateBuffer();
//                            data.write(jsonNode.toString().getBytes(StandardCharsets.UTF_8));
//                            holder.length = data.readableByteCount();
//                            holder.dataBuffer=data;
//                        });
//
//                    }catch (Exception e){
//                        if(e.getCause() instanceof ServiceException){
//                            ServiceException e1 = (ServiceException) e.getCause();
//                            return handleFailedRequest(exchange, JSONObject.toJSONString(CommonResponse.error(e1.getCode(), e1.getMessage())));
//                        }
//                        return handleFailedRequest(exchange, JSONObject.toJSONString(CommonResponse.error(SYSTEM_ERROR.getCode(), SYSTEM_ERROR.getMessage())));
//                    }
//
//
//                    ServerHttpRequestDecorator decorator = newDecorator(exchange,holder.length, Flux.just(holder.dataBuffer));
//                    return chain.filter(exchange.mutate().request(decorator).build());
//
//                }));
//
//        return mono;
//    }
//
//    //修改form参数
//    private Mono<Void> fileRequest(MediaType contentType,ServerWebExchange exchange, GatewayFilterChain chain){
//        return DataBufferUtils.join(exchange.getRequest().getBody())
//                .flatMap(dataBuffer -> {
//                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(bytes);
//                    DataBufferUtils.release(dataBuffer);
//                    //  new String(bytes);
//                    //添加一些自定义参数，或者校验
//                    String oldBody = addPara(contentType.toString(), new String(bytes));
//
//                    byte[] bytes1 = oldBody.getBytes();
//                    //这里截取数组是因为 form请求体数据末尾有--/r/n，这里需要截一段然后拼接自定义的数据。数据格式可以看文章末尾的链接
//                    byte[] bytes2 = byteMerger(Arrays.copyOf(bytes,bytes.length-4), bytes1);
//                    Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
//                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes2);
//                        DataBufferUtils.retain(buffer);
//                        return Mono.just(buffer);
//                    });
//                    ServerHttpRequestDecorator mutatedRequest = newDecorator(exchange,bytes2.length,cachedFlux);
//                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
//                    return ServerRequest.create(mutatedExchange, MESSAGE_READERS)
//                            .bodyToMono(byte[].class)
//                            .then(chain.filter(mutatedExchange));
//                });
//    }
//
//    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
//        byte[] bt3 = new byte[bt1.length+bt2.length];
//        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
//        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
//        return bt3;
//    }
//
//    public ServerHttpRequestDecorator newDecorator(ServerWebExchange exchange,long dataLength,Flux<DataBuffer> body){
//        return new ServerHttpRequestDecorator(
//                exchange.getRequest()) {
//            @Override
//            public HttpHeaders getHeaders() {
//                //数据长度变了以后 需要修改header里的数据，不然接收数据时会异常
//                //我看别人说删除会自动补充数据长度，但我这个版本不太行
////				long contentLength = headers.getContentLength();
//                HttpHeaders httpHeaders = new HttpHeaders();
//                httpHeaders.putAll(super.getHeaders());
////				if (contentLength > 0) {
//                httpHeaders.setContentLength(dataLength);
////				} else {
////					httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
////				}
//                return httpHeaders;
//            }
//
//            @Override
//            public Flux<DataBuffer> getBody() {
//                return body;
//            }
//        };
//    }
//
//
//    /**
//     * 修改form参数
//     * @param contentType 请求类型
//     * @param bodyString  请求body信息
//     */
//    @SneakyThrows
//    public static String addPara(String contentType, String bodyString) {
//        StringBuffer stringBuffer = new StringBuffer();
//
//        String boundary = contentType.substring(contentType.lastIndexOf("boundary=") + 9);//获取随机字符传信息
//        String boundary_end = StrUtil.format("--{}--\r\n", boundary);
//        Map<String, Object> formMap = Maps.newHashMap();
//        /**
//         *
//         * 根据自己需求进行对bodyString信息修改，例如下面，根据boundary对传入的bodyString进行了分割
//         *  String[] split = bodyString.split(boundary);
//         *  然后将修改完后的信息封装到formMap中，需要注意的是，file文件需要以new FileResource(file, fileName)的形式当作value放到formMap中
//         */
//        String part = "^\r\nContent-Disposition: form-data; name=\"([^/?]+)\"\r\n\r\n([^/?]+)\r\n--?$";
//        Pattern r = Pattern.compile(part);
//        String[] split = bodyString.split(boundary);
//        for(int x=1;x<split.length-1;x++){
//            Matcher m = r.matcher(split[x]);
//            if(m.find()){
//                String name = m.group(1);
//                String value = m.group(2);
//                System.out.println("name:"+name+" value:"+value);
////				formMap.put(name,value);
//            }
//        }
//
//        formMap.put("test","添加自定义参数");
//        formMap.put("tcu",22222);
//        Integer count =0;
//        for (Map.Entry<String, Object> entry : formMap.entrySet()) {
//            stringBuffer.append(appendPart(boundary, entry.getKey(), entry.getValue(),count));
//            count++;
//        }
//        stringBuffer.append(boundary_end);//拼接结束信息
//        log.info(stringBuffer.toString());
//
//        return stringBuffer.toString();
//    }
//
//    /**
//     * 添加Multipart表单的数据项
//     *
//     * @param boundary      随机串信息
//     * @param formFieldName 表单名
//     * @param value         值，可以是普通值、资源（如文件等）
//     */
//    private static String appendPart(String boundary, String formFieldName, Object value,Integer count) {
//        StringBuffer stringBuffer = new StringBuffer();
//        // 多资源
//        if (value instanceof MultiResource) {
//            for (Resource subResource : (MultiResource) value) {
//                appendPart(boundary, formFieldName, subResource,count);
//            }
//            return stringBuffer.toString();
//        }
//
//        if(count!=0){
//            stringBuffer.append("--").append(boundary).append(StrUtil.CRLF);
//        }else{
//            stringBuffer.append(StrUtil.CRLF);
////			stringBuffer.append(boundary).append(StrUtil.CRLF);
//        }
//
//        if (value instanceof Resource) {
//            // 文件资源（二进制资源）
//            final Resource resource = (Resource) value;
//            final String fileName = resource.getName();
//            stringBuffer.append(StrUtil.format(CONTENT_DISPOSITION_FILE_TEMPLATE, formFieldName, ObjectUtil.defaultIfNull(fileName, formFieldName)));
//            // 根据name的扩展名指定互联网媒体类型，默认二进制流数据
//            stringBuffer.append(StrUtil.format(CONTENT_TYPE_FILE_TEMPLATE, HttpUtil.getMimeType(fileName, "application/octet-stream")));
//        } else {
//            // 普通数据
//            stringBuffer.append(StrUtil.format(CONTENT_DISPOSITION_TEMPLATE, formFieldName)).append(value);
//        }
//        stringBuffer.append(StrUtil.CRLF);
//        return stringBuffer.toString();
//    }
//
////    /**
////     * 修改 x-www-form-urlencoded参数
////     * @param exchange
////     * @param chain
////     * @return
////     */
////    private Mono<Void> xwFromBody(ServerWebExchange exchange, GatewayFilterChain chain){
////        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
////        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(Mono::just);
////        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
////        HttpHeaders headers = new HttpHeaders();
////        headers.putAll(exchange.getRequest().getHeaders());
////        headers.remove(HttpHeaders.CONTENT_LENGTH);
////        MyCachedBodyOutputMessage outputMessage = new MyCachedBodyOutputMessage(exchange, headers);
////        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
////        requestBuilder.headers(k -> k.remove("Content-length"));
////
////        return bodyInserter.insert(outputMessage, new BodyInserterContext())
////                .then(Mono.defer(() -> {
////                    Flux<DataBuffer> body = outputMessage.getBody();
////                    DataBufferHolder holder = new DataBufferHolder();
////                    changeParamByXwForm(body,holder,outputMessage);
////                    ServerHttpRequestDecorator decorator = newDecorator(exchange,holder.length, Flux.just(holder.dataBuffer));
////                    return chain.filter(exchange.mutate().request(decorator).build());
////                }));
////    }
////    //修改 x-www-form-urlencoded参数
////    private void changeParamByXwForm(Flux<DataBuffer> body,DataBufferHolder holder,MyCachedBodyOutputMessage outputMessage){
////        body.subscribe(dataBuffer -> {
////            int len = dataBuffer.readableByteCount();
////            holder.length = len;
////            byte[] bytes = new byte[len];
////            dataBuffer.read(bytes);
////            DataBufferUtils.release(dataBuffer);
////            String oldBody = new String(bytes, StandardCharsets.UTF_8);
////            //直接拼接要改的数据就行
////            oldBody+="&tcu=123888";
////            DataBuffer data = outputMessage.bufferFactory().allocateBuffer();
////            data.write(oldBody.getBytes(StandardCharsets.UTF_8));
////            holder.length = data.readableByteCount();
////            holder.dataBuffer=data;
////        });
////    }
//
//
//
//}
