package com.geekonup.service.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 飞书自动化视频分析响应结果
 */
@Data
public class VideoAnalysisResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // TODO: 根据实际需求添加字段
    // 例如：
    // private String summary;           // 素材总结
    // private String contentTags;       // 内容标签
    // private String highlightSlices;   // 高光切片
    // private Integer emotionalIntensity; // 情感强度
    // private Integer antiShakeScore;   // 防抖评分
    // private Integer exposureScore;     // 曝光、动态范围评分
    // private Integer focusScore;       // 焦点、清晰度评分
}

