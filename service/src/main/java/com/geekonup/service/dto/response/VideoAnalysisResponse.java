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
     * 剪辑结论
     * 综合视频内容价值、剪辑可行性给出最终剪辑建议，用于快速筛选是否进入剪辑流程。
     * 可选值：必剪/可剪/不剪
     */
    private String editConclusion;

    /**
     * 剪辑优先级
     * 在"可剪"及"必剪"视频中，用于区分剪辑顺序与资源投入优先级。
     * P0 表示应优先投入剪辑资源，P1 表示可按计划排期，P2 表示作为补充或备用素材
     * 可选值：P0/P1/P2
     */
    private String editPriority;

    /**
     * 视频总结
     * 对视频内容进行概述，说明视频中发生了什么
     */
    private String videoSummary;

    /**
     * 内容标签
     * 认知价值（判断 / 决策 / 方法）信任价值（团队 / 日常 / 真实感）
     */
    private String contentTags;

    /**
     * 核心价值
     * 提炼视频中对目标受众最有价值的核心信息，用一句话说明该视频为何值得被剪辑。
     */
    private String coreValue;

    /**
     * 剪辑结构建议
     * 基于视频内容特点，给出适合的剪辑结构方向，明确剪辑节奏与结构类型。
     */
    private String editStructureSuggestion;

    /**
     * 高光切片
     * 标记最具代表性，最出片的高能片段。
     * 例如：0:25 - 0:43 / 1:45 - 2:08 / 3:17 - 3:33
     */
    private String highlightSlices;

    /**
     * 情感强度
     * L0-无感：平淡叙述或空镜
     * L1-微笑/认可：轻度积极反应
     * L2-大笑/惊喜：高笑点或意外转折
     * L3-感动/共鸣：强烈情感流露，能引发评论互动
     * L4-愤怒/争议：具有话题性的观点冲突
     * 可选值：L0/L1/L2/L3/L4
     */
    private String emotionalIntensity;
}

