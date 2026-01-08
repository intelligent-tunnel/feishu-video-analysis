package com.geekonup.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // 通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    
    // 业务错误码
    VIDEO_NOT_FOUND(1001, "视频文件不存在"),
    VIDEO_ANALYSIS_FAILED(1002, "视频分析失败"),
    NAS_CONNECTION_FAILED(1003, "NAS连接失败"),
    AI_SERVICE_ERROR(1004, "AI服务错误");
    
    private final Integer code;
    private final String message;
    
    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

