package com.luyublog.gateway.handler;

import cn.hutool.json.JSONUtil;
import com.luyublog.gateway.common.config.OrderConstant;
import com.luyublog.gateway.common.exception.BaseException;
import com.luyublog.gateway.common.exception.ErrorCode;
import com.luyublog.gateway.common.result.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @description: 网关统一异常处理
 * 參考：https://stackoverflow.com/questions/70304712
 * @author: east
 * @date: 2023/9/14
 */
@Component
@Slf4j
public class GatewayExceptionHandler implements WebExceptionHandler, Ordered {
    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
        ServerHttpResponse response = serverWebExchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        BaseResult<Object> result = new BaseResult<Object>();


        result.setCode(HttpStatus.BAD_REQUEST.value());
        if (throwable instanceof BaseException) {
            result.setMsg(((BaseException) throwable).getMsg());
        } else {
            log.error("system error:", throwable);
            result.setMsg(ErrorCode.ERROR_UNKNOWN.getMsg());
        }

        DataBuffer resp = response.bufferFactory().wrap(JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(resp));
    }

    @Override
    public int getOrder() {
        return OrderConstant.EXCEPTION_HANDLER.getOrder();
    }
}
