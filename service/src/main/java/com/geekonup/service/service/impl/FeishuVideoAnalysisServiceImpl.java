package com.geekonup.service.service.impl;

import com.geekonup.service.dto.request.VideoAnalyzeRequest;
import com.geekonup.service.dto.response.StatusCode;
import com.geekonup.service.dto.response.VideoAnalysisResponse;
import com.geekonup.service.service.FeishuVideoAnalysisService;
import cn.hutool.core.util.StrUtil;
import com.geekonup.service.util.FfmpegUtil;
import com.geekonup.service.util.FeishuBitableUtil;
import com.geekonup.service.util.VideoAnalysisAiUtil;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞书视频分析服务实现类
 */
@Slf4j
@Service
public class FeishuVideoAnalysisServiceImpl implements FeishuVideoAnalysisService {

    // FFmpeg路径
    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    // 目标分辨率
    @Value("${ffmpeg.compression.resolution}")
    private String targetResolution;

    // 目标帧率
    @Value("${ffmpeg.compression.frame-rate}")
    private int targetFrameRate;

    // 质量因子
    @Value("${ffmpeg.compression.crf}")
    private int crf;

    // 视频文件目录
    @Value("${ffmpeg.video-dir}")
    private String videoDir;

    // 压缩超时时间（秒）
    @Value("${ffmpeg.timeout:1800}")
    private int compressionTimeout;

    // 飞书多维表格配置
    @Value("${feishu.bitable.app-id:}")
    private String feishuAppId;

    @Value("${feishu.bitable.app-secret:}")
    private String feishuAppSecret;

    @Value("${feishu.bitable.app-token:}")
    private String feishuAppToken;

    @Value("${feishu.bitable.table-id:}")
    private String feishuTableId;

    @Value("${feishu.bitable.field-mapping.edit-conclusion:剪辑结论}")
    private String fieldEditConclusion;

    @Value("${feishu.bitable.field-mapping.edit-priority:剪辑优先级}")
    private String fieldEditPriority;

    @Value("${feishu.bitable.field-mapping.video-summary:视频总结}")
    private String fieldVideoSummary;

    @Value("${feishu.bitable.field-mapping.content-tags:内容标签}")
    private String fieldContentTags;

    @Value("${feishu.bitable.field-mapping.core-value:核心价值}")
    private String fieldCoreValue;

    @Value("${feishu.bitable.field-mapping.edit-structure-suggestion:剪辑结构建议}")
    private String fieldEditStructureSuggestion;

    @Value("${feishu.bitable.field-mapping.highlight-slices:高光切片}")
    private String fieldHighlightSlices;

    @Value("${feishu.bitable.field-mapping.emotional-intensity:情感强度}")
    private String fieldEmotionalIntensity;

    @Value("${feishu.bitable.field-mapping.markdown-report:分析报告}")
    private String fieldMarkdownReport;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String aiBaseUrl;

    // 视频理解模型
    @Value("${ai.step1-model:qwen-vl-max}")
    private String aiStep1Model;

    // 文本结构化模型
    @Value("${ai.step2-model:qwen-max}")
    private String aiStep2Model;

    // Prompt 文件路径
    @Value("${ai.prompt1-path:D:\\nas\\prompt\\prompt1.txt}")
    private String aiPrompt1Path;

    @Value("${ai.prompt2-path:D:\\nas\\prompt\\prompt2.txt}")
    private String aiPrompt2Path;

    private VideoAnalysisAiUtil aiUtil;

    private VideoAnalysisAiUtil getAiUtil() {
        if (aiUtil == null) {
            if (aiApiKey == null || aiApiKey.isBlank()) {
                throw new IllegalStateException("AI API Key 未配置");
            }
            aiUtil = new VideoAnalysisAiUtil(aiApiKey, aiBaseUrl);
        }
        return aiUtil;
    }

    @Override
    public VideoAnalysisResponse handleAnalysis(VideoAnalyzeRequest request) {
        log.info("收到视频分析请求: videoName={}, recordId={}",
                request.videoName(), request.recordId());

        // 立即返回，异步处理
        VideoAnalysisResponse response = new VideoAnalysisResponse();
        response.setStatusCode(new StatusCode(200, "请求已接收，正在处理中..."));

        // 异步处理视频分析和更新飞书多维表格
        processAnalysisAsync(request);
        return response;
    }

    /**
     * 异步处理视频分析和更新飞书多维表格
     */
    @Async
    public void processAnalysisAsync(VideoAnalyzeRequest request) {
        try {
            log.info("开始异步处理视频分析: videoName={}, recordId={}",
                    request.videoName(), request.recordId());

            // 1. 处理视频查找和压缩
            VideoCompressionResult compressionResult = processVideoCompression(request.videoName());
            if (!compressionResult.isSuccess()) {
                log.error("视频压缩失败: {}", compressionResult.getErrorMessage());
                updateFeishuBitableWithError(request.recordId(), compressionResult.getErrorMessage());
                return;
            }

            // 2. 调用AI分析
            VideoAnalysisData analysisData = analyzeVideoWithAI(
                    compressionResult.getCompressedVideoPath());
            if (!analysisData.isSuccess()) {
                log.error("AI分析失败: {}", analysisData.getErrorMessage());
                updateFeishuBitableWithError(request.recordId(), analysisData.getErrorMessage());
                return;
            }

            // 3. 更新飞书多维表格
            updateFeishuBitable(request.recordId(), analysisData);

            log.info("视频分析完成并已更新飞书多维表格: recordId={}", request.recordId());
        } catch (Exception e) {
            log.error("异步处理视频分析时发生异常: recordId={}", request.recordId(), e);
            updateFeishuBitableWithError(request.recordId(), "处理失败: " + e.getMessage());
        }
    }

    /**
     * 更新飞书多维表格（成功情况）
     */
    private void updateFeishuBitable(String recordId, VideoAnalysisData analysisData) {
        if (StrUtil.isBlank(recordId)) {
            log.warn("记录ID为空，跳过更新飞书多维表格");
            return;
        }

        if (StrUtil.isBlank(feishuAppId) || StrUtil.isBlank(feishuAppSecret) ||
                StrUtil.isBlank(feishuAppToken) || StrUtil.isBlank(feishuTableId)) {
            log.warn("飞书多维表格配置不完整，跳过更新");
            return;
        }

        try {
            Map<String, Object> fields = new HashMap<>();
            // 保存阶段一的 Markdown 分析报告
            fields.put(fieldMarkdownReport, analysisData.getMarkdownReport());

            boolean success = FeishuBitableUtil.updateRecord(
                    feishuAppToken, feishuTableId, recordId, fields, feishuAppId, feishuAppSecret);

            if (success) {
                log.info("成功更新飞书多维表格: recordId={}", recordId);
            } else {
                log.error("更新飞书多维表格失败: recordId={}", recordId);
            }
        } catch (Exception e) {
            log.error("更新飞书多维表格时发生异常: recordId={}", recordId, e);
        }
    }

    /**
     * 更新飞书多维表格（错误情况）
     */
    private void updateFeishuBitableWithError(String recordId, String errorMessage) {
        if (StrUtil.isBlank(recordId)) {
            return;
        }

        if (StrUtil.isBlank(feishuAppId) || StrUtil.isBlank(feishuAppSecret) ||
                StrUtil.isBlank(feishuAppToken) || StrUtil.isBlank(feishuTableId)) {
            return;
        }

        try {
            // 当前仅记录日志，后续若需要可将错误信息写回飞书多维表格
            log.warn("分析失败，错误信息: {}", errorMessage);
        } catch (Exception e) {
            log.error("更新飞书多维表格错误信息时发生异常: recordId={}", recordId, e);
        }
    }

    /**
     * 处理视频查找和压缩
     *
     * @param videoName 视频名称
     * @return 压缩结果，包含成功状态和压缩后的视频路径
     */
    private VideoCompressionResult processVideoCompression(String videoName) {
        // 检查视频名称是否为空
        if (videoName == null || videoName.isBlank()) {
            log.warn("视频名称为空");
            return VideoCompressionResult.failure("视频名称不能为空");
        }

        // 检查目录
        Path nasPath = Paths.get(videoDir);
        File nasDirectory = nasPath.toFile();

        if (!nasDirectory.exists()) {
            log.warn("视频目录不存在: {}", videoDir);
            return VideoCompressionResult.failure("视频目录不存在");
        }

        if (!nasDirectory.isDirectory()) {
            log.warn("指定路径不是目录: {}", videoDir);
            return VideoCompressionResult.failure("指定路径不是目录");
        }

        // 查找视频文件
        String videoNameWithoutExt = removeFileExtension(videoName);
        File videoFile = FfmpegUtil.findVideoFile(videoDir, videoNameWithoutExt);

        if (videoFile == null) {
            log.warn("未找到视频文件: videoName={}, 目录={}", videoName, videoDir);
            return VideoCompressionResult.failure("没有找到该名称的视频文件");
        }

        // 记录原文件大小
        long originalFileSize = videoFile.length();
        double originalSizeMB = originalFileSize / 1024.0 / 1024.0;
        log.info("原视频文件大小: {} MB", String.format("%.2f", originalSizeMB));

        // AI模型限制：只能接收100M以内的视频
        long maxSizeForAI = 100L * 1024 * 1024; // 100MB
        long skipCompressionThreshold = 99L * 1024 * 1024; // 99MB，小于此值不压缩

        // 如果视频小于99MB，直接返回原文件路径，不压缩
        if (originalFileSize < skipCompressionThreshold) {
            log.info("视频小于99MB，无需压缩，直接使用原文件: {}", videoFile.getAbsolutePath());
            return VideoCompressionResult.success(videoFile.getAbsolutePath());
        }

        // 根据原文件大小动态选择压缩参数，确保压缩后 < 100MB
        CompressionConfig config = determineCompressionConfig(originalFileSize, maxSizeForAI);
        log.info("视频需要压缩，使用配置：分辨率={}, 帧率={}, CRF={}, 超时={}秒",
                config.resolution, config.frameRate, config.crf, config.timeout);

        if (!FfmpegUtil.checkFfmpegAvailable(ffmpegPath)) {
            log.warn("FFmpeg 不可用，请检查配置路径: {}", ffmpegPath);
            return VideoCompressionResult.failure("FFmpeg 不可用，请检查配置");
        }

        String inputVideoPath = videoFile.getAbsolutePath();
        String outputVideoPath = generateCompressedVideoPath(inputVideoPath, config);

        log.info("开始压缩视频: {} -> {}", inputVideoPath, outputVideoPath);
        log.info("压缩参数: 分辨率={}, 帧率={}, CRF={}, 超时={}秒",
                config.resolution, config.frameRate, config.crf, config.timeout);

        // 执行视频压缩
        boolean compressSuccess = FfmpegUtil.compressVideo(
                ffmpegPath,
                inputVideoPath,
                outputVideoPath,
                config.resolution,
                config.frameRate,
                config.crf,
                config.timeout
        );

        if (!compressSuccess) {
            log.error("视频压缩失败: {}", inputVideoPath);
            return VideoCompressionResult.failure("视频压缩失败");
        }

        // 验证输出文件是否存在
        File outputFile = new File(outputVideoPath);
        if (!outputFile.exists() || !outputFile.isFile()) {
            log.error("压缩后的视频文件不存在: {}", outputVideoPath);
            return VideoCompressionResult.failure("压缩后的视频文件生成失败");
        }

        // 记录压缩后文件大小
        long compressedFileSize = outputFile.length();
        double compressedSizeMB = compressedFileSize / 1024.0 / 1024.0;
        double compressionRatio = (1.0 - (double) compressedFileSize / originalFileSize) * 100;
        log.info("视频压缩成功: {}", outputVideoPath);
        log.info("压缩后文件大小: {} MB, 压缩率: {}%",
                String.format("%.2f", compressedSizeMB),
                String.format("%.2f", compressionRatio));

        // 验证压缩后文件是否超过100MB限制
        if (compressedFileSize > maxSizeForAI) {
            log.warn("压缩后文件大小 {} MB 仍超过100MB限制，可能需要进一步压缩", 
                    String.format("%.2f", compressedSizeMB));
            // 这里可以选择返回失败，或者继续使用（取决于你的需求）
            // 当前先记录警告，继续使用
        }

        return VideoCompressionResult.success(outputVideoPath);
    }

    /**
     * 调用AI分析视频（阶段一：生成 Markdown 分析报告）
     *
     * @param compressedVideoPath 压缩后的视频文件路径
     * @return 分析结果数据（包含 Markdown 报告）
     */
    private VideoAnalysisData analyzeVideoWithAI(String compressedVideoPath) {
        log.info("开始AI分析视频（阶段一：Markdown）: {}", compressedVideoPath);
        try {
            // 调用AI工具类分析视频，返回 Markdown 格式的分析报告
            String markdownReport = getAiUtil().analyzeVideoToMarkdown(
                    compressedVideoPath,
                    aiPrompt1Path,
                    aiStep1Model
            );

            // 创建分析结果数据，保存 Markdown 报告
            VideoAnalysisData data = VideoAnalysisData.success();
            data.setMarkdownReport(markdownReport);

            log.info("AI分析完成（阶段一：Markdown）");
            return data;
        } catch (Exception e) {
            log.error("AI分析视频时发生异常", e);
            return VideoAnalysisData.failure("AI分析失败: " + e.getMessage());
        }
    }

    /**
     * 根据原文件大小动态确定压缩配置
     * 目标：确保压缩后文件 < 100MB
     * 
     * 基于测试数据：
     * - 5.78GB → 219MB (1920:1080, 24fps, CRF=23): 压缩率约 3.79%
     * - 5.78GB → 80.37MB (1280:720, 24fps, CRF=25): 压缩率约 1.39%
     */
    private CompressionConfig determineCompressionConfig(long originalFileSize, long targetMaxSize) {
        double originalSizeMB = originalFileSize / 1024.0 / 1024.0;
        
        // 根据文件大小选择压缩策略
        if (originalSizeMB < 200) {
            // 200MB以下：轻微压缩，保持较高画质
            return new CompressionConfig("1920:1080", 24, 23, 1800);
        } else if (originalSizeMB < 1000) {
            // 200MB-1GB：中等压缩
            return new CompressionConfig("1280:720", 24, 25, 2400);
        } else if (originalSizeMB < 3000) {
            // 1GB-3GB：较强压缩
            return new CompressionConfig("1280:720", 24, 25, 3600);
        } else if (originalSizeMB < 6000) {
            // 3GB-6GB：强压缩，降低帧率
            return new CompressionConfig("1280:720", 20, 25, 3600);
        } else {
            // 6GB以上：最强压缩，最低帧率
            return new CompressionConfig("1280:720", 18, 25, 3600);
        }
    }

    /**
     * 压缩配置内部类
     */
    private static class CompressionConfig {
        final String resolution;
        final int frameRate;
        final int crf;
        final int timeout;

        CompressionConfig(String resolution, int frameRate, int crf, int timeout) {
            this.resolution = resolution;
            this.frameRate = frameRate;
            this.crf = crf;
            this.timeout = timeout;
        }
    }

    /**
     * 生成压缩后的视频文件路径
     * 格式：原文件名_compressed_分辨率_帧率-release.mp4
     * 例如：video.mp4 -> video_compressed_1280x720_24fps-release.mp4
     * 保存位置：与原视频同一目录
     */
    private String generateCompressedVideoPath(String originalPath, CompressionConfig config) {
        Path path = Paths.get(originalPath);
        String parentDir = path.getParent().toString();
        String fileName = path.getFileName().toString();
        String fileNameWithoutExt = removeFileExtension(fileName);

        String resolutionForFileName = config.resolution.replace(":", "x");

        // 生成新文件名，添加 -release 后缀
        String newFileName = String.format("%s_compressed_%s_%dfps-release.mp4",
                fileNameWithoutExt, resolutionForFileName, config.frameRate);

        return Paths.get(parentDir, newFileName).toString();
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

    /**
     * 视频压缩结果
     */
    @Getter
    private static class VideoCompressionResult {
        private final boolean success;
        private final String compressedVideoPath;
        private final String errorMessage;

        private VideoCompressionResult(boolean success, String compressedVideoPath, String errorMessage) {
            this.success = success;
            this.compressedVideoPath = compressedVideoPath;
            this.errorMessage = errorMessage;
        }

        public static VideoCompressionResult success(String compressedVideoPath) {
            return new VideoCompressionResult(true, compressedVideoPath, null);
        }

        public static VideoCompressionResult failure(String errorMessage) {
            return new VideoCompressionResult(false, null, errorMessage);
        }
    }

    /**
     * 视频分析数据
     */
    @Data
    private static class VideoAnalysisData {
        private boolean success;
        private String errorMessage;
        // 阶段一的 Markdown 分析报告
        private String markdownReport;

        public static VideoAnalysisData success() {
            VideoAnalysisData data = new VideoAnalysisData();
            data.success = true;
            return data;
        }

        public static VideoAnalysisData failure(String errorMessage) {
            VideoAnalysisData data = new VideoAnalysisData();
            data.success = false;
            data.errorMessage = errorMessage;
            return data;
        }
    }
}

