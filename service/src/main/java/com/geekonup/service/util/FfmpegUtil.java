package com.geekonup.service.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg工具类
 * 
 * 常用参数说明：
 * -i: 指定输入文件
 * -vf: 视频滤镜（如缩放、裁剪等）
 * -r: 设置帧率
 * -c:v: 视频编码器（libx264 是最常用的 H.264 编码器）
 * -crf: 恒定质量因子（18-28，数值越小质量越高，文件越大）
 * -preset: 编码速度预设（ultrafast/fast/medium/slow/veryslow）
 * -b:v: 视频码率（如 5M 表示 5Mbps）
 * -c:a: 音频编码器（aac 是常用格式）
 * -b:a: 音频码率（如 128k）
 * -pix_fmt: 像素格式（yuv420p 兼容性最好）
 * -movflags +faststart: 支持在线播放边下边播
 */
@Slf4j
public class FfmpegUtil {

    /**
     * 压缩视频到指定分辨率、帧率和质量
     * 
     * @param ffmpegPath FFmpeg 可执行文件路径（如 "D:\\ffmpeg\\bin\\ffmpeg.exe"）
     * @param inputVideoPath 输入视频文件路径
     * @param outputVideoPath 输出视频文件路径
     * @param resolution 目标分辨率（如 "1920:1080" 或 "1280:720"）
     * @param frameRate 目标帧率（如 24）
     * @param crf 质量因子（18-28，推荐 23）
     * @param timeoutSeconds 超时时间（秒），0 表示不设置超时
     * @return 是否压缩成功
     */
    public static boolean compressVideo(String ffmpegPath, String inputVideoPath, 
                                       String outputVideoPath, String resolution, 
                                       int frameRate, int crf, int timeoutSeconds) {
        try {
            // 构建 FFmpeg 命令
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputVideoPath);
            
            // 视频滤镜：缩放分辨率，保持宽高比，并确保宽高为偶数（libx264要求）
            command.add("-vf");
            command.add("scale=" + resolution + ":force_original_aspect_ratio=decrease,scale='trunc(iw/2)*2':'trunc(ih/2)*2'");
            
            // 设置帧率
            command.add("-r");
            command.add(String.valueOf(frameRate));
            
            // 视频编码器：使用 H.264
            command.add("-c:v");
            command.add("libx264");
            
            // 编码预设：slow 表示更高质量的压缩
            command.add("-preset");
            command.add("slow");
            
            // 质量因子：CRF 23 是推荐的平衡值
            command.add("-crf");
            command.add(String.valueOf(crf));
            
            // 像素格式：yuv420p 兼容性最好
            command.add("-pix_fmt");
            command.add("yuv420p");
            
            // 编码配置
            command.add("-profile:v");
            command.add("high");
            command.add("-level");
            command.add("4.2");
            
            // 支持在线播放
            command.add("-movflags");
            command.add("+faststart");
            
            // 音频编码：使用 AAC，码率 128k
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("128k");
            
            // 覆盖输出文件（如果存在）
            command.add("-y");
            
            // 输出文件路径
            command.add(outputVideoPath);
            
            log.info("执行 FFmpeg 命令: {}", String.join(" ", command));
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出信息（在单独线程中，避免阻塞）
            StringBuilder outputBuilder = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("FFmpeg 输出: {}", line);
                        outputBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("读取 FFmpeg 输出时发生异常", e);
                }
            });
            outputThread.start();
            
            // 等待进程完成（带超时控制）
            int exitCode;
            if (timeoutSeconds > 0) {
                boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    log.error("视频压缩超时（{}秒），终止进程", timeoutSeconds);
                    process.destroyForcibly();
                    outputThread.interrupt();
                    // 等待进程真正终止
                    try {
                        process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception e) {
                        // 忽略
                    }
                    return false;
                }
                exitCode = process.exitValue();
            } else {
                exitCode = process.waitFor();
            }
            
            // 等待输出线程完成
            try {
                outputThread.join(5000);
            } catch (InterruptedException e) {
                log.warn("等待输出线程完成时被中断", e);
            }
            
            if (exitCode == 0) {
                log.info("视频压缩成功: {} -> {}", inputVideoPath, outputVideoPath);
                return true;
            } else {
                log.error("视频压缩失败，退出码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("执行视频压缩时发生异常", e);
            return false;
        }
    }
    
    /**
     * 使用固定码率压缩视频（CBR - Constant Bitrate）
     * 
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @param inputVideoPath 输入视频文件路径
     * @param outputVideoPath 输出视频文件路径
     * @param resolution 目标分辨率
     * @param frameRate 目标帧率
     * @param videoBitrate 视频码率（如 "5M" 表示 5Mbps）
     * @return 是否压缩成功
     */
    public static boolean compressVideoWithBitrate(String ffmpegPath, String inputVideoPath,
                                                   String outputVideoPath, String resolution,
                                                   int frameRate, String videoBitrate) {
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputVideoPath);
            
            command.add("-vf");
            command.add("scale=" + resolution);
            
            command.add("-r");
            command.add(String.valueOf(frameRate));
            
            command.add("-c:v");
            command.add("libx264");
            
            // 固定码率设置
            command.add("-b:v");
            command.add(videoBitrate);
            command.add("-maxrate");
            command.add(videoBitrate);
            command.add("-bufsize");
            command.add(String.valueOf(Integer.parseInt(videoBitrate.replace("M", "")) * 2) + "M");
            
            command.add("-preset");
            command.add("slow");
            
            command.add("-pix_fmt");
            command.add("yuv420p");
            
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("128k");
            
            command.add("-y");
            command.add(outputVideoPath);
            
            log.info("执行 FFmpeg 命令（固定码率）: {}", String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg 输出: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("视频压缩成功（固定码率）: {} -> {}", inputVideoPath, outputVideoPath);
                return true;
            } else {
                log.error("视频压缩失败，退出码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("执行视频压缩时发生异常", e);
            return false;
        }
    }
    
    /**
     * 检查 FFmpeg 是否可用
     * 
     * @param ffmpegPath FFmpeg 可执行文件路径
     * @return 是否可用
     */
    public static boolean checkFfmpegAvailable(String ffmpegPath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath, "-version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("检查 FFmpeg 可用性时发生异常", e);
            return false;
        }
    }
    
    /**
     * 在目录中查找视频文件
     * 
     * @param directory 目录路径
     * @param videoNameWithoutExt 视频名称（不含扩展名）
     * @return 找到的视频文件，如果未找到返回 null
     */
    public static File findVideoFile(String directory, String videoNameWithoutExt) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        // 支持的视频格式
        String[] videoExtensions = {".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".m4v"};
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    String fileNameWithoutExt = removeFileExtension(fileName);
                    
                    if (fileNameWithoutExt.equals(videoNameWithoutExt)) {
                        // 检查是否是视频文件
                        String extension = getFileExtension(fileName).toLowerCase();
                        for (String videoExt : videoExtensions) {
                            if (videoExt.equals("." + extension)) {
                                return file;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 移除文件扩展名
     */
    private static String removeFileExtension(String fileName) {
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
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
}

