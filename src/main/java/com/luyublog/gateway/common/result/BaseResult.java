package com.luyublog.gateway.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: east
 * @date: 2023/9/16
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResult<T> {
    /**
     * 状态码
     */
    private Integer code;

    /**
     * 说明信息
     */
    private String msg;

    /**
     * 错误code
     */
    private String appCode;

    /**
     * 服务名/url
     */
    private String path;

    /**
     * 返回数据
     */
    private T data;
}
