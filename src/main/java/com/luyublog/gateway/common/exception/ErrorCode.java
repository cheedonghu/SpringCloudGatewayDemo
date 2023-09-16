package com.luyublog.gateway.common.exception;

import lombok.Getter;

/**
 * @description:
 * @author: east
 * @date: 2023/9/16
 */
@Getter
public enum ErrorCode {
    ERROR_PARAM("ERORR001", "参数异常"),
    ERROR_UNKNOWN("ERORRXXX", "系统异常"),
    ;
    private String code;
    private String msg;

    ErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
