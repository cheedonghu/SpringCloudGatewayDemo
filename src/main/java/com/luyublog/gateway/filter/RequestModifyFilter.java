package com.luyublog.gateway.filter;

import com.luyublog.gateway.service.FormDataRequestBodyRewriteService;
import com.luyublog.gateway.service.JsonRequestBodyRewriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
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
public class RequestModifyFilter implements GlobalFilter, Ordered {
    @Autowired
    private ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilter;
    @Autowired
    private JsonRequestBodyRewriteService jsonRequestBodyRewriteService;
    @Autowired
    private FormDataRequestBodyRewriteService formDataRequestBodyRewriteService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            // 纯json报文处理逻辑
            return modifyRequestBodyFilter
                    .apply(
                            new ModifyRequestBodyGatewayFilterFactory.Config()
                                    .setRewriteFunction(byte[].class, byte[].class, jsonRequestBodyRewriteService))
                    .filter(exchange, chain);
        } else if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)) {
            // form表单数据处理
            return modifyRequestBodyFilter
                    .apply(
                            new ModifyRequestBodyGatewayFilterFactory.Config()
                                    .setRewriteFunction(byte[].class, byte[].class, formDataRequestBodyRewriteService))
                    .filter(exchange, chain);
        } else {
            return filter(exchange, chain);
        }

    }

    @Override
    public int getOrder() {
        return 20; // The order in which you want this filter to execute
    }
}
