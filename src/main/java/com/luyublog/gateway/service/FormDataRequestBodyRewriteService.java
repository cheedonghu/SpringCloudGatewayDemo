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
import java.util.HashMap;
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
 * todo 参考@RequestPart注解的实现可能有更好的方法
 *
 * TODO 不同版本的boundary还不同，有的没有WebKitFormBoundary，startIndex也需要根据实际情况调整
 *
 * @author: east
 * @date: 2023/9/15
 */
@Service
@Slf4j
public class FormDataRequestBodyRewriteService implements RewriteFunction<byte[], byte[]> {
    private final String BOUNDARY_PREFIX_IN_CONTENT_TYPE = "boundary=";
    private final String BOUNDARY_PREFIX = "--";
    private final String BOUNDARY_SUFFIX = "--\r\n";

    @Override
    public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] body) {
//        Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();


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
        String[] split = request.split(BOUNDARY_PREFIX + randomStr);

        // 表单转换为Map
        HashMap<String, String> formDataMap = new HashMap<>();
        getFormData(formDataMap, split);

        // 获取其中json数据并替换
        JSONObject originJson = JSONUtil.parseObj(formDataMap.get("json"));
        String originJsonString = formDataMap.get("json");
        log.info("开始处理原json数据:{}", originJson.toString());
        originJson.set("newKey", "newValue");
        log.info("新json数据:{}", originJson.toString());
        String modifiedJsonString = originJson.toString();

        // 将新数据替换掉原表单中的数据
        finalResultString = StrUtil.replace(request, originJsonString, modifiedJsonString);

        return Mono.just(finalResultString.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * 将表单转为map数据
     *
     * @param resultMap     map
     * @param splitFormData 分割结果
     */
    private void getFormData(HashMap<String, String> resultMap, String[] splitFormData) {
        String keyString = "\r\nContent-Disposition: form-data; name=\"(.*?)\"";
        String valueString = "\r\n\r\n(.*?)\r\n";
        for (String formInfo : splitFormData) {
            Pattern keyPattern = Pattern.compile(keyString);
            // DOTALL兼容因string采用utf16编码导致出现终止符问题
            Pattern valuePattern = Pattern.compile(valueString, Pattern.DOTALL);
            Matcher keyMatcher = keyPattern.matcher(formInfo);
            Matcher valueMatcher = valuePattern.matcher(formInfo);
            if (keyMatcher.find() && valueMatcher.find()) {
                resultMap.put(keyMatcher.group(1), valueMatcher.group(1));
            }
        }

    }
}
