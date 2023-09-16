package com.luyublog.gateway.common.exception;

import lombok.Builder;
import lombok.Data;

/**
 * @description: 參考：https://juejin.cn/post/6963443170185052168
 * @author: east
 * @date: 2023/9/16
 */
@Data
@Builder
public class BaseException extends RuntimeException {
    private String appCode;
    private String path;
    private String msg;
    private Object data;
}
