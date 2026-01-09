package com.geekonup.service.dto.request;

/**
 * 视频分析请求
 */
public record VideoAnalyzeRequest(
        /* 视频名称 */
        String videoName,
        /* 飞书多维表格记录ID（用于异步更新结果） */
        String recordId
) {
}

