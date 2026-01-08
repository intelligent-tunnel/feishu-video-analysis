package com.geekonup.service.service.impl;

import com.geekonup.service.dto.request.VideoAnalyzeRequest;
import com.geekonup.service.dto.response.VideoAnalysisResponse;
import com.geekonup.service.service.FeishuVideoAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 飞书视频分析服务实现类
 */
@Slf4j
@Service
public class FeishuVideoAnalysisServiceImpl implements FeishuVideoAnalysisService {

    @Override
    public VideoAnalysisResponse handleAnalysis(VideoAnalyzeRequest request) {
        log.info("开始处理视频分析请求: videoName={}, question={}, prompt={}",
                request.videoName(), request.question(), request.prompt());

        // TODO: 这里先做占位，后续接入实际的分析逻辑（调用 NAS / AI 等）
        VideoAnalysisResponse response = new VideoAnalysisResponse();

        log.info("视频分析完成: videoName={}", request.videoName());
        return response;
    }
}

