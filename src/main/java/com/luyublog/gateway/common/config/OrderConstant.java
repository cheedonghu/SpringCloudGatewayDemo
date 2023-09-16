package com.luyublog.gateway.common.config;

import lombok.Getter;

/**
 * @description:
 * @author: east
 * @date: 2023/9/16
 */
@Getter
public enum OrderConstant {
    REQUEST_MODIFY_FILTER(2),
    RESPONSE_LOG_FILTER(-100),
    EXCEPTION_HANDLER(RESPONSE_LOG_FILTER.order + 1),
    ;
    private Integer order;

    OrderConstant(Integer order) {
        this.order = order;
    }
}
