package com.geekonup.service.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书多维表格工具类
 * 用于更新多维表格中的记录
 */
@Slf4j
public class FeishuBitableUtil {

    /**
     * Token 缓存（key: appId, value: {token, expireTime}）
     */
    private static final Map<String, TokenCache> TOKEN_CACHE = new ConcurrentHashMap<>();

    /**
     * Token 缓存类
     */
    private static class TokenCache {
        private final String token;
        private final long expireTime;

        public TokenCache(String token, long expireTime) {
            this.token = token;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTime;
        }

        public String getToken() {
            return token;
        }
    }

    /**
     * 获取 tenant_access_token
     * 
     * @param appId 飞书应用的 App ID
     * @param appSecret 飞书应用的 App Secret
     * @return tenant_access_token
     */
    public static String getTenantAccessToken(String appId, String appSecret) {
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(appSecret)) {
            throw new IllegalArgumentException("App ID 和 App Secret 不能为空");
        }

        // 检查缓存
        TokenCache cached = TOKEN_CACHE.get(appId);
        if (cached != null && !cached.isExpired()) {
            log.debug("使用缓存的 tenant_access_token");
            return cached.getToken();
        }

        // 获取新的 token
        String url = FeishuUtil.buildApiUrl("/auth/v3/tenant_access_token/internal");
        
        Map<String, String> body = new HashMap<>();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);

        try {
            JSONObject response = FeishuUtil.post(url, null, body);
            String token = response.getStr("tenant_access_token");
            int expire = response.getInt("expire", 7200); // 默认2小时
            
            // 缓存 token（提前5分钟过期，避免边界问题）
            long expireTime = System.currentTimeMillis() + (expire - 300) * 1000L;
            TOKEN_CACHE.put(appId, new TokenCache(token, expireTime));
            
            log.info("获取 tenant_access_token 成功，过期时间: {} 秒", expire);
            return token;
        } catch (Exception e) {
            log.error("获取 tenant_access_token 失败", e);
            throw new RuntimeException("获取 tenant_access_token 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新多维表格记录
     * 
     * @param appToken 多维表格的 App Token
     * @param tableId 数据表的 Table ID
     * @param recordId 记录ID
     * @param fields 要更新的字段（key: 字段名, value: 字段值）
     * @param tenantAccessToken tenant_access_token
     * @return 是否更新成功
     */
    public static boolean updateRecord(String appToken, String tableId, String recordId,
                                       Map<String, Object> fields, String tenantAccessToken) {
        if (StrUtil.isBlank(appToken) || StrUtil.isBlank(tableId) || StrUtil.isBlank(recordId)) {
            log.error("参数不完整: appToken={}, tableId={}, recordId={}", appToken, tableId, recordId);
            return false;
        }

        if (fields == null || fields.isEmpty()) {
            log.warn("没有要更新的字段");
            return false;
        }

        String url = FeishuUtil.buildApiUrl(
                String.format("/bitable/v1/apps/%s/tables/%s/records/%s", appToken, tableId, recordId));

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> fieldsMap = new HashMap<>();
        
        // 将字段值转换为飞书多维表格格式
        // 飞书多维表格字段格式：
        // - 文本类型: {"text": "值"}
        // - 数字类型: {"number": 值}
        // - 多行文本: {"text": "值"}
        // - 单选: {"text": "值"}
        // - 多选: {"text": ["值1", "值2"]}
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            if (fieldValue == null) {
                // 空值处理
                fieldsMap.put(fieldName, null);
                continue;
            }
            
            Map<String, Object> fieldData = new HashMap<>();
            
            // 判断字段类型并构建对应的值格式
            if (fieldValue instanceof Integer || fieldValue instanceof Long) {
                // 数字类型
                fieldData.put("number", fieldValue);
            } else if (fieldValue instanceof Double || fieldValue instanceof Float) {
                // 浮点数类型
                fieldData.put("number", fieldValue);
            } else {
                // 默认作为文本类型处理
                fieldData.put("text", String.valueOf(fieldValue));
            }
            
            fieldsMap.put(fieldName, fieldData);
        }
        
        requestBody.put("fields", fieldsMap);

        try {
            FeishuUtil.put(url, tenantAccessToken, requestBody);
            log.info("更新多维表格记录成功: recordId={}", recordId);
            return true;
        } catch (Exception e) {
            log.error("更新多维表格记录失败: recordId={}", recordId, e);
            return false;
        }
    }

    /**
     * 更新多维表格记录（自动获取 token）
     * 
     * @param appToken 多维表格的 App Token
     * @param tableId 数据表的 Table ID
     * @param recordId 记录ID
     * @param fields 要更新的字段（key: 字段名, value: 字段值）
     * @param appId 飞书应用的 App ID
     * @param appSecret 飞书应用的 App Secret
     * @return 是否更新成功
     */
    public static boolean updateRecord(String appToken, String tableId, String recordId,
                                       Map<String, Object> fields, String appId, String appSecret) {
        try {
            String token = getTenantAccessToken(appId, appSecret);
            return updateRecord(appToken, tableId, recordId, fields, token);
        } catch (Exception e) {
            log.error("更新多维表格记录失败", e);
            return false;
        }
    }
}

