package com.luyublog.gateway.filter;

import com.luyublog.gateway.service.RequestBodyRewriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @description:
 * @author: east
 * @date: 2023/9/15
 */
@Component
@Slf4j
public class AnotherFilter implements GlobalFilter, Ordered {
    @Autowired
    private ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;
    @Autowired
    private RequestBodyRewriteService requestBodyRewrite;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return modifyRequestBodyFilter
                .apply(
                        new ModifyRequestBodyGatewayFilterFactory.Config()
                                .setRewriteFunction(byte[].class, byte[].class, requestBodyRewrite))
                .filter(exchange, chain);
    }

    @Override
    public int getOrder() {
        return 20; // The order in which you want this filter to execute
    }
}
