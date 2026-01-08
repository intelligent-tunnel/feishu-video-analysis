package com.geekonup.service.service;

import com.geekonup.service.dto.request.VideoAnalyzeRequest;
import com.geekonup.service.dto.response.VideoAnalysisResponse;

/**
 * 飞书视频分析服务
 */
public interface FeishuVideoAnalysisService {

    VideoAnalysisResponse handleAnalysis(VideoAnalyzeRequest request);

}

