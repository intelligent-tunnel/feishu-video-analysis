package com.geekonup.service.dto.request;

/**
 * 视频分析请求
 */
public record VideoAnalyzeRequest(
        /* 视频名称 */
        String videoName,
        /* 提问 */
        String question,
        /* Prompt */
        String prompt
) {
}

