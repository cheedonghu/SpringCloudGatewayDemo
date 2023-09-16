package com.luyublog.gateway.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @description:
 * @author: east
 * @date: 2023/9/15
 */
@Service
@Slf4j
public class JsonRequestBodyRewriteService implements RewriteFunction<byte[], byte[]> {
    @Override
    public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] body) {
        JSONObject request = JSONUtil.parseObj(body);
        log.info("原始报文:{}", request.toString());
        try {
            request.set("empId", "2345");
            request.set("department", "Engineering");

            log.info("修改后报文:{}", request);
            return Mono.just(request.toString().getBytes());
        } catch (Exception e) {
            log.error("修改报文时出错", e);
            throw e;
        }
    }
}
