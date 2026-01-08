package com.geekonup.service.service.impl;

import com.geekonup.service.dto.request.VideoAnalyzeRequest;
import com.geekonup.service.dto.response.StatusCode;
import com.geekonup.service.dto.response.VideoAnalysisResponse;
import com.geekonup.service.service.FeishuVideoAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        // 检查视频名称是否为空
        String videoName = request.videoName();
        if (videoName == null || videoName.isBlank()) {
            log.warn("视频名称为空");
            VideoAnalysisResponse errorResponse = new VideoAnalysisResponse();
            errorResponse.setStatusCode(new StatusCode(500, "视频名称不能为空"));
            return errorResponse;
        }

        // 在 D:\nas 目录下查找文件
        String nasDir = "D:\\nas";
        Path nasPath = Paths.get(nasDir);
        File nasDirectory = nasPath.toFile();

        // 检查目录是否存在
        if (!nasDirectory.exists() || !nasDirectory.isDirectory()) {
            log.warn("NAS目录不存在或不是目录: {}", nasDir);
            VideoAnalysisResponse errorResponse = new VideoAnalysisResponse();
            errorResponse.setStatusCode(new StatusCode(500, "没有找到该名称的文件"));
            return errorResponse;
        }

        // 去掉 request 中 videoName 的扩展名
        String videoNameWithoutExt = removeFileExtension(videoName);

        // 查找与 videoName（去掉扩展名后）匹配的文件
        File[] files = nasDirectory.listFiles();
        boolean fileFound = false;

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileNameWithoutExt = removeFileExtension(file.getName());
                    if (fileNameWithoutExt.equals(videoNameWithoutExt)) {
                        fileFound = true;
                        break;
                    }
                }
            }
        }

        if (!fileFound) {
            log.warn("未找到文件: videoName={}", videoName);
            VideoAnalysisResponse errorResponse = new VideoAnalysisResponse();
            errorResponse.setStatusCode(new StatusCode(500, "没有找到该名称的文件"));
            return errorResponse;
        }

        // TODO 占位，后续调用 NAS / AI 等
        VideoAnalysisResponse response = new VideoAnalysisResponse();
        response.setStatusCode(new StatusCode(200, "操作成功"));
        response.setSummary("素材总结占位");
        response.setContentTags("内容标签占位");
        response.setHighlightSlices("高光切片占位");
        response.setEmotionalIntensity(0);
        response.setAntiShakeScore(0);
        response.setExposureScore(0);
        response.setFocusScore(0);

        return response;
    }

    // 去掉文件扩展名
    private String removeFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
}

