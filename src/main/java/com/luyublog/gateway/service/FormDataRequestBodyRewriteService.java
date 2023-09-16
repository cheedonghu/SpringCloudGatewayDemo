package com.luyublog.gateway.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * 获取表单内的json键数据并进行修改。这里有几个前提条件
 * 1. 表单只包含两部分：json字符串和其他类型数据
 * 2. json的表单key值为json
 * <p>
 * 参考：https://blog.csdn.net/qq_36966137/article/details/128536391
 *
 * @author: east
 * @date: 2023/9/15
 */
@Service
@Slf4j
public class FormDataRequestBodyRewriteService implements RewriteFunction<byte[], byte[]> {
    private final String BOUNDARY_PREFIX_IN_CONTENT_TYPE = "----WebKitFormBoundary";
    private final String BOUNDARY_PREFIX_IN_FORM_DATA = "------WebKitFormBoundary";
    private final String BOUNDARY_SUFFIX = "--\r\n";

    @Override
    public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] body) {
        String finalResultString = "";

        // 将表单转为字符串格式从而根据boundary分割表单数据。注意这里不能用默认编码
        String request = StrUtil.str(body, StandardCharsets.ISO_8859_1);

        // 获取boundary的随机字符信息
        String contentType = exchange.getRequest().getHeaders().getContentType().toString();
        String randomStr = contentType.substring(contentType.indexOf(BOUNDARY_PREFIX_IN_CONTENT_TYPE) + BOUNDARY_PREFIX_IN_CONTENT_TYPE.length());

        // 这里和前端约定json数据的表单key为json
        String keyPart = "^\r\nContent-Disposition: form-data; name=\"json\"";
        Pattern r = Pattern.compile(keyPart);

        // 根据表单内分割线进行分割。并通过关键段落keyPart来找到目标json数据
        String[] split = request.split(BOUNDARY_PREFIX_IN_FORM_DATA + randomStr);
        for (int x = 0; x < split.length - 1; x++) {
            Matcher m = r.matcher(split[x]);
            if (m.find()) {
                // 找到了json报文部分数据
                String originalJsonString = split[x];

                // 找到 JSON 数据的起始和结束位置
                int startIndex = originalJsonString.indexOf("{\"");
                int endIndex = originalJsonString.indexOf("\"}") + 2;
                // 提取 JSON 数据
                String jsonData = originalJsonString.substring(startIndex, endIndex);
                log.info("原始报文为：{}", jsonData);

                JSONObject jsonObject = JSONUtil.parseObj(jsonData);
                jsonObject.set("empId", "2345");
                jsonObject.set("department", "Engineering");
                String modifiedString = originalJsonString.substring(0, startIndex) + jsonObject + originalJsonString.substring(endIndex);
                log.info("修改后报文为：{}", modifiedString);

                // 重新组装split数组
                finalResultString = finalResultString + modifiedString + BOUNDARY_PREFIX_IN_FORM_DATA + randomStr;
            } else {
                // 重组表单数据
                finalResultString = finalResultString + split[x] + BOUNDARY_PREFIX_IN_FORM_DATA + randomStr;
            }
        }

        // 补上最后一截数据
        finalResultString = finalResultString + BOUNDARY_SUFFIX;

        return Mono.just(finalResultString.getBytes(StandardCharsets.ISO_8859_1));
    }
}
