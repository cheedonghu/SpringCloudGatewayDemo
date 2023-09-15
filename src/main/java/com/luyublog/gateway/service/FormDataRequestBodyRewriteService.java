package com.luyublog.gateway.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 获取表单内的json键数据并进行修改
 * 参考：https://blog.csdn.net/qq_36966137/article/details/128536391
 *
 * @author: east
 * @date: 2023/9/15
 */
@Service
@Slf4j
public class FormDataRequestBodyRewriteService implements RewriteFunction<byte[], byte[]> {
    private final String BOUNDARY_PREFIX = "----WebKitFormBoundary";

    private final String EXACT_BOUNDARY_PREFIX = "------WebKitFormBoundary";

    @Override
    public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] body) {

        String modifyString = "";

        String data = StrUtil.str(body, StandardCharsets.ISO_8859_1);

        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        //获取随机字符传信息
        String randomStr = contentType.toString().substring(contentType.toString().indexOf(BOUNDARY_PREFIX) + BOUNDARY_PREFIX.length(), contentType.toString().length());

        String part = "^\r\nContent-Disposition: form-data; name=\"json\"";
        Pattern r = Pattern.compile(part);
        String[] split = data.split(EXACT_BOUNDARY_PREFIX + randomStr);
        for (int x = 0; x < split.length - 1; x++) {
            Matcher m = r.matcher(split[x]);
            if (m.find()) {
                String originalString = split[x];

                // 找到 JSON 数据的起始和结束位置
                int startIndex = originalString.indexOf("{\"");
                int endIndex = originalString.indexOf("\"}") + 2;
                // 提取 JSON 数据
                String jsonData = originalString.substring(startIndex, endIndex);
                log.info("原始报文为：{}", jsonData);

                JSONObject jsonObject = JSONUtil.parseObj(jsonData);
                jsonObject.set("empId", "2345");
                jsonObject.set("department", "Engineering");
                String resultString = originalString.substring(0, startIndex) + jsonObject + originalString.substring(endIndex);
                log.info("修改后报文为：{}", resultString);

                // 重新组装split数组
                modifyString = modifyString + resultString + EXACT_BOUNDARY_PREFIX + randomStr;
            } else {
                modifyString = modifyString + split[x] + EXACT_BOUNDARY_PREFIX + randomStr;
            }
        }

        String endStr = "--\r\n";
        modifyString = modifyString + endStr;

        return Mono.just(modifyString.getBytes(StandardCharsets.ISO_8859_1));
    }
}
