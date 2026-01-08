package com.geekonup.service.dto.response;

import lombok.Data;

/**
 * 视频分析响应
 */
@Data
public class VideoAnalysisResponse {

    /**
     * 接口返回状态码
     */
    private StatusCode statusCode;

    /**
     * 素材总结
     * 描述：总结视频素材内容
     */
    private String summary;

    /**
     * 内容标签
     * 描述：
     * 1. 核心论点
     * 2. 数据事实
     * 3. 总结升华
     */
    private String contentTags;

    /**
     * 高光切片
     * 描述：
     * 标记最具代表性，最出片的高能片段。例如：
     * 1.  0:25 - 0:43
     * 2.  1:45 - 2:08
     * 3.  3:17 - 3:33
     */
    private String highlightSlices;

    /**
     * 情感强度
     * 描述：
     * L0-无感：平淡叙述或空镜
     * L1-微笑/认可：轻度积极反应
     * L2-大笑/惊喜：高笑点或意外转折
     * L3-感动/共鸣：强烈情感流露，能引发评论互动
     * L4-愤怒/争议：具有话题性的观点冲突
     */
    private Integer emotionalIntensity;

    /**
     * 防抖评分
     */
    private Integer antiShakeScore;

    /**
     * 曝光、动态范围评分
     */
    private Integer exposureScore;

    /**
     * 焦点、清晰度评分
     */
    private Integer focusScore;
}

