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
 * @description: 获取表单内的json键数据并进行修改
 * @author: east
 * @date: 2023/9/15
 */
@Service
@Slf4j
public class FormDataRequestBodyRewriteService implements RewriteFunction<byte[], byte[]> {
    @Override
    public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] body) {
        JSONObject map = JSONUtil.parseObj(body);
        log.info("原始报文:{}", map.toString());
        try {
//            Map<String, Object> map = gson.fromJson(body, Map.class);
            map.set("empId", "2345");
            map.set("department", "Engineering");

            log.info("修改后报文:{}", map.toString());
            return Mono.just(map.toString().getBytes());
        } catch (Exception ex) {
            log.error(
                    "An error occured while transforming the request body in class RequestBodyRewrite. {}",
                    ex);

            // Throw custom exception here
            throw new RuntimeException(
                    "An error occured while transforming the request body in class RequestBodyRewrite.");
        }
    }
}
