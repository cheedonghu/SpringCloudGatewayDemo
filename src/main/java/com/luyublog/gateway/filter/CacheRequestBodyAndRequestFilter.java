package com.luyublog.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 参考：https://github.com/spring-cloud/spring-cloud-gateway/issues/1587
 */
@Component
@Slf4j
public class CacheRequestBodyAndRequestFilter implements GlobalFilter, Ordered {

    public static final String CACHED_REQUEST_BODY_X_WWW_FORM_URLENCODED_MAP_KEY = "cachedXWwwFormUrlEncodedMap";
    public static final String CACHED_REQUEST_BODY_FORM_DATA_MAP_KEY = "cachedRequestBodyFormDataMap";

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Supplier<? extends Mono<? extends Void>> supplier = () -> chain.filter(exchange);

        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
            return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange,
                    (serverHttpRequest) -> ServerRequest
                            .create(exchange.mutate().request(serverHttpRequest).build(), HandlerStrategies.withDefaults().messageReaders())
                            .bodyToMono(new ParameterizedTypeReference<MultiValueMap<String, String>>() {
                            })
                            .doOnNext(map -> {
                                exchange.getAttributes().put(CACHED_REQUEST_BODY_X_WWW_FORM_URLENCODED_MAP_KEY, map);
                                log.info("x-www-urlencoded: {}", toStringToStringListMap(map));
                            })).then(Mono.defer(supplier));
        } else if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
            return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange,
                    (serverHttpRequest) -> ServerRequest
                            .create(exchange.mutate().request(serverHttpRequest).build(), HandlerStrategies.withDefaults().messageReaders())
                            .bodyToMono(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {
                            })
                            .doOnNext(map -> {
                                exchange.getAttributes().put(CACHED_REQUEST_BODY_FORM_DATA_MAP_KEY, map);
                                log.info("from-data: {}", toStringToStringListMap(map));
                            })).then(Mono.defer(supplier));
        } else {
            return chain.filter(exchange);
        }
    }

    private Map<String, List<String>> toStringToStringListMap(MultiValueMap<String, ?> multiValueMap) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends List<?>> entry : multiValueMap.entrySet()) {
            String key = entry.getKey();
            List<?> valueList = entry.getValue();
            map.put(key, valueList.stream().map(value -> {
                if (value instanceof String) {
                    return (String) value;
                } else if (value instanceof FormFieldPart) {
                    return ((FormFieldPart) value).value();
                } else {
                    return "";
                }
            }).collect(Collectors.toList()));
        }

        return map;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // After RemoveCachedBodyFilter, before AdaptCachedBodyGlobalFilter
    }

}
