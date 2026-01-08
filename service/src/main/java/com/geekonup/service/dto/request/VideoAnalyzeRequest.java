package com.geekonup.service.dto.request;

/**
 * 飞书自动化视频分析请求体
 */
public record VideoAnalyzeRequest(
        String videoName,
        String question,
        String prompt
) {
}

