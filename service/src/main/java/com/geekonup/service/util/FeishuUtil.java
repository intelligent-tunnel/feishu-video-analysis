package com.geekonup.service.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书相关的通用工具类（鉴权、HTTP 请求封装等）。
 * <p>
 * 使用 Hutool 的 HttpUtil 进行网络请求，提供优雅的链式调用。
 * <p>
 * 后续如果需要：
 * - 刷新 tenant_access_token / user_access_token
 * - 计算签名（有些回调需要）
 * - 统一错误码解析
 * 可在此类继续扩展。
 */
@Slf4j
public class FeishuUtil {

    /**
     * 飞书 OpenAPI 基础地址
     */
    private static final String FEISHU_API_BASE = "https://open.feishu.cn/open-apis";

    private FeishuUtil() {
        // utility class
    }

    /**
     * 构造鉴权 Header Map。
     *
     * @param token tenant_access_token 或 user_access_token
     * @return Map headers
     */
    public static Map<String, String> buildAuthHeaders(String token) {
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("token 不能为空");
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json; charset=utf-8");
        return headers;
    }

    /**
     * 发送 GET 请求到飞书 OpenAPI。
     *
     * @param url    完整的 API URL（包含基础路径）
     * @param token  tenant_access_token 或 user_access_token
     * @param params 查询参数（可选）
     * @return JSONObject 响应体
     */
    public static JSONObject get(String url, String token, Map<String, Object> params) {
        return executeRequest(HttpRequest.get(url)
                .headerMap(buildAuthHeaders(token), true)
                .form(params != null ? params : new HashMap<>()));
    }

    /**
     * 发送 GET 请求到飞书 OpenAPI（无查询参数）。
     *
     * @param url   完整的 API URL
     * @param token tenant_access_token 或 user_access_token
     * @return JSONObject 响应体
     */
    public static JSONObject get(String url, String token) {
        return get(url, token, null);
    }

    /**
     * 发送 POST 请求到飞书 OpenAPI。
     *
     * @param url   完整的 API URL
     * @param token tenant_access_token 或 user_access_token
     * @param body  请求体对象（会自动序列化为 JSON）
     * @return JSONObject 响应体
     */
    public static JSONObject post(String url, String token, Object body) {
        return executeRequest(HttpRequest.post(url)
                .headerMap(buildAuthHeaders(token), true)
                .body(JSONUtil.toJsonStr(body)));
    }

    /**
     * 发送 POST 请求到飞书 OpenAPI（无请求体）。
     *
     * @param url   完整的 API URL
     * @param token tenant_access_token 或 user_access_token
     * @return JSONObject 响应体
     */
    public static JSONObject post(String url, String token) {
        return executeRequest(HttpRequest.post(url)
                .headerMap(buildAuthHeaders(token), true));
    }

    /**
     * 发送 PUT 请求到飞书 OpenAPI。
     *
     * @param url   完整的 API URL
     * @param token tenant_access_token 或 user_access_token
     * @param body  请求体对象（会自动序列化为 JSON）
     * @return JSONObject 响应体
     */
    public static JSONObject put(String url, String token, Object body) {
        return executeRequest(HttpRequest.put(url)
                .headerMap(buildAuthHeaders(token), true)
                .body(JSONUtil.toJsonStr(body)));
    }

    /**
     * 发送 DELETE 请求到飞书 OpenAPI。
     *
     * @param url   完整的 API URL
     * @param token tenant_access_token 或 user_access_token
     * @return JSONObject 响应体
     */
    public static JSONObject delete(String url, String token) {
        return executeRequest(HttpRequest.delete(url)
                .headerMap(buildAuthHeaders(token), true));
    }

    /**
     * 执行 HTTP 请求并统一处理响应。
     *
     * @param request HttpRequest 对象
     * @return JSONObject 响应体
     */
    private static JSONObject executeRequest(HttpRequest request) {
        try {
            log.debug("发送请求: {} {}", request.getMethod(), request.getUrl());
            HttpResponse response = request.execute();
            String body = response.body();
            log.debug("响应状态: {}, 响应体: {}", response.getStatus(), body);

            JSONObject jsonResponse = JSONUtil.parseObj(body);
            
            // 检查飞书返回的错误码
            Integer code = jsonResponse.getInt("code");
            if (!isSuccess(code)) {
                String msg = jsonResponse.getStr("msg", "未知错误");
                log.warn("飞书 API 调用失败: code={}, msg={}", code, msg);
                throw new FeishuApiException(code, msg);
            }

            return jsonResponse;
        } catch (FeishuApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求飞书 API 异常", e);
            throw new RuntimeException("请求飞书 API 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建完整的飞书 API URL。
     *
     * @param path API 路径，例如："/bitable/v1/apps/:app_token/tables/:table_id/records"
     * @return 完整的 URL
     */
    public static String buildApiUrl(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("API 路径不能为空");
        }
        // 确保路径以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return FEISHU_API_BASE + path;
    }

    /**
     * 简单校验飞书 OpenAPI 返回是否成功。
     *
     * @param code 飞书返回的 code，0 表示成功
     * @return 是否成功
     */
    public static boolean isSuccess(Integer code) {
        return code != null && code == 0;
    }

    /**
     * 飞书 API 异常类
     */
    public static class FeishuApiException extends RuntimeException {
        private final Integer code;

        public FeishuApiException(Integer code, String message) {
            super(message);
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }
    }
}

