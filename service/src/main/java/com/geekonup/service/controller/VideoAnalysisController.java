package com.geekonup.service.controller;

import com.geekonup.common.result.Result;
import com.geekonup.service.dto.response.VideoAnalysisResponse;
import com.geekonup.service.service.FeishuVideoAnalysisService;
import com.geekonup.service.dto.request.VideoAnalyzeRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 视频分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoAnalysisController {

    @Value("${feishu.verify-token}")
    private String verifyToken;

    private final FeishuVideoAnalysisService videoAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<Result<VideoAnalysisResponse>> analyze(@RequestBody VideoAnalyzeRequest request,
                                                                 HttpServletRequest httpRequest) {
        log.info("======== 收到分析请求 ========");
        log.info("请求IP: {}", httpRequest.getRemoteAddr());
        log.info("请求体: videoName={}, question={}, prompt={}",
                request.videoName(), request.question(), request.prompt());
        // 简单校验
        String tokenFromHeader = httpRequest.getHeader("X-Feishu-Token");
        if (!StringUtils.hasText(tokenFromHeader) || !tokenFromHeader.equals(verifyToken)) {
            log.warn("Token 校验失败: {}", tokenFromHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error("invalid token"));
        }

        return ResponseEntity.ok(Result.success(videoAnalysisService.handleAnalysis(request)));
    }
}

