package com.geekonup.service.util;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * 视频分析AI工具类
 * 用于调用AI模型分析视频内容
 */
@Slf4j
public class VideoAnalysisAiUtil {

    private final OpenAIClient openAIClient;

    public VideoAnalysisAiUtil(String apiKey, String baseUrl) {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }
    
    /**
     * 静态方法：使用默认配置创建实例
     * 参考 AiParseBusinessTimeUtil 的实现方式
     */
    public static VideoAnalysisAiUtil createDefault(String apiKey, String baseUrl) {
        return new VideoAnalysisAiUtil(apiKey, baseUrl);
    }

    /**
     * 分析视频并返回 Markdown 格式的分析报告（阶段一）
     *
     * @param videoFilePath 视频文件路径
     * @param prompt1Path   阶段一 prompt 文件路径
     * @param step1Model    阶段一模型名称（视觉模型）
     * @return Markdown 格式的分析报告
     */
    public String analyzeVideoToMarkdown(String videoFilePath,
                                         String prompt1Path,
                                         String step1Model) {
        try {
            File videoFile = new File(videoFilePath);
            if (!videoFile.exists() || !videoFile.isFile()) {
                throw new IllegalArgumentException("视频文件不存在: " + videoFilePath);
            }

            log.info("开始分析视频（两阶段）: {}", videoFilePath);
            log.info("视频文件大小: {} MB", String.format("%.2f", videoFile.length() / 1024.0 / 1024.0));

            // ============ 阶段一：视频 -> Markdown ============
            String prompt1 = Files.readString(new File(prompt1Path).toPath());

            // system 使用 prompt1，user 只传视频相关信息
            ChatCompletionCreateParams.Builder step1Builder = ChatCompletionCreateParams.builder();
            step1Builder.addSystemMessage(prompt1);

            String step1UserMessage = buildUserMessage(videoFile);

            ChatCompletionCreateParams step1Params = step1Builder
                    .addUserMessage(step1UserMessage)
                    .model(step1Model)
                    .build();

            ChatCompletion step1Completion = openAIClient.chat().completions().create(step1Params);
            String mdContent = step1Completion.choices().get(0).message().content().orElse(null);
            if (mdContent == null || mdContent.isEmpty()) {
                throw new RuntimeException("阶段一：AI模型返回内容为空");
            }

            log.info("阶段一分析完成，开始保存 Markdown 文档");

            // 将 Markdown 文档保存到与视频同目录、同名的 .md 文件
            File videoParent = videoFile.getParentFile();
            String videoName = videoFile.getName();
            int dotIndex = videoName.lastIndexOf('.');
            String baseName = dotIndex > 0 ? videoName.substring(0, dotIndex) : videoName;
            File mdFile = new File(videoParent, baseName + ".md");
            Files.writeString(mdFile.toPath(), mdContent);
            log.info("Markdown 分析文档已保存: {}", mdFile.getAbsolutePath());

            log.info("视频分析完成（阶段一：Markdown）");
            return mdContent;

        } catch (Exception e) {
            log.error("分析视频时发生异常（两阶段）", e);
            throw new RuntimeException("视频分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建系统提示词
     *
     * 已被两阶段分析流程替代，保留方法仅为兼容或后续参考，当前未使用。
     */
    @SuppressWarnings("unused")
    private String buildSystemPrompt() {
        return """
                您是一位从事自媒体行业15年的资深视频内容分析专家，擅长分析爆款视频。现在你的任务是对根据提供的视频分析报告进行深度分析，并按照严格的JSON格式返回分析结果。
                
                ## 输出格式要求
                
                你必须严格按照以下JSON格式输出，不要输出任何其他文字说明：
                
                ```json
                {
                  "editConclusion": "必剪/可剪/不剪",
                  "editPriority": "P0/P1/P2",
                  "videoSummary": "视频内容概述",
                  "contentTags": "内容标签",
                  "coreValue": "核心价值",
                  "editStructureSuggestion": "剪辑结构建议",
                  "highlightSlices": "高光切片时间点",
                  "emotionalIntensity": "L0/L1/L2/L3/L4"
                }
                ```
                
                ## 字段说明与要求
                
                ### 1. editConclusion（剪辑结论）
                - **含义**：综合视频内容价值、剪辑可行性给出最终剪辑建议，用于快速筛选是否进入剪辑流程
                - **可选值**：必须为以下之一
                  - "必剪"：视频内容价值高，强烈建议剪辑
                  - "可剪"：视频有一定价值，可以剪辑
                  - "不剪"：视频价值较低，不建议剪辑
                - **判断标准**：
                  - 必剪：内容精彩、有亮点、适合传播
                  - 可剪：内容尚可，有一定价值
                  - 不剪：内容平淡、无亮点、不适合传播
                
                ### 2. editPriority（剪辑优先级）
                - **含义**：在"可剪"及"必剪"视频中，用于区分剪辑顺序与资源投入优先级
                - **可选值**：必须为以下之一
                  - "P0"：应优先投入剪辑资源，内容价值最高
                  - "P1"：可按计划排期，内容价值中等
                  - "P2"：作为补充或备用素材，内容价值一般
                - **注意**：如果 editConclusion 为"不剪"，则 editPriority 可以为空或"P2"
                
                ### 3. videoSummary（视频总结）
                - **含义**：对视频内容进行概述，说明视频中发生了什么
                - **要求**：
                  - 简洁明了，100-200字
                  - 涵盖视频的主要内容、场景、人物、事件
                  - 用第三人称客观描述
                
                ### 4. contentTags（内容标签）
                - **含义**：标注视频的认知价值和信任价值
                - **格式**：用"/"分隔多个标签
                - **认知价值标签**（可选）：
                  - 判断：包含观点判断、价值判断
                  - 决策：包含决策过程、选择建议
                  - 方法：包含方法技巧、操作指南
                - **信任价值标签**（可选）：
                  - 团队：展现团队协作、团队文化
                  - 日常：展现日常工作、生活场景
                  - 真实感：展现真实状态、未加修饰
                - **示例**："判断/决策/团队/真实感"
                
                ### 5. coreValue（核心价值）
                - **含义**：提炼视频中对目标受众最有价值的核心信息，用一句话说明该视频为何值得被剪辑
                - **要求**：
                  - 一句话概括，30-50字
                  - 突出视频的独特价值和吸引力
                  - 说明为什么这个视频值得被剪辑和传播
                
                ### 6. editStructureSuggestion（剪辑结构建议）
                - **含义**：基于视频内容特点，给出适合的剪辑结构方向，明确剪辑节奏与结构类型
                - **要求**：
                  - 50-100字
                  - 建议剪辑结构类型（如：线性叙事、倒叙、对比、蒙太奇等）
                  - 说明剪辑节奏（快节奏/慢节奏/节奏变化）
                  - 给出具体的剪辑思路
                
                ### 7. highlightSlices（高光切片）
                - **含义**：标记最具代表性，最出片的高能片段
                - **格式**：时间点用" - "连接，多个片段用" / "分隔
                - **时间格式**：分钟:秒数（如：0:25 - 0:43）
                - **要求**：
                  - 至少标记2-3个高光片段
                  - 时间点要准确
                  - 片段要有代表性，能体现视频亮点
                - **示例**："0:25 - 0:43 / 1:45 - 2:08 / 3:17 - 3:33"
                
                ### 8. emotionalIntensity（情感强度）
                - **含义**：评估视频的情感强度和感染力
                - **可选值**：必须为以下之一
                  - "L0"：无感 - 平淡叙述或空镜，情感平淡
                  - "L1"：微笑/认可 - 轻度积极反应，轻微情感波动
                  - "L2"：大笑/惊喜 - 高笑点或意外转折，中等情感强度
                  - "L3"：感动/共鸣 - 强烈情感流露，能引发评论互动，高情感强度
                  - "L4"：愤怒/争议 - 具有话题性的观点冲突，极高情感强度
                - **判断标准**：
                  - L0：内容平淡，无情感波动
                  - L1：有轻微积极反应，但不够强烈
                  - L2：有明显的情感反应，能引起注意
                  - L3：情感强烈，能引发共鸣和互动
                  - L4：情感极强，有争议性或话题性
                
                ## 分析要求
                
                1. **仔细观看视频**：全面理解视频内容、场景、人物、情节
                2. **客观分析**：基于视频实际内容进行分析，不要主观臆测
                3. **严格格式**：必须严格按照JSON格式输出，字段名必须完全匹配
                4. **值域限制**：所有字段的值必须在规定的可选值范围内
                5. **内容质量**：确保分析内容准确、有价值、可操作
                
                ## 输出要求
                
                - 只输出JSON，不要任何其他文字
                - JSON格式必须正确，可以被解析
                - 所有字段都必须有值（editPriority在"不剪"时可以为空）
                - 字符串值不要包含换行符，使用空格分隔
                """;
    }

    /**
     * 构建用户消息（包含视频文件）
     * 注意：当前 OpenAI Java SDK 可能不完全支持视频，这里先使用文本描述的方式
     * 后续可以根据实际API支持情况调整
     */
    private String buildUserMessage(File videoFile) {
        try {
            // 构建消息内容
            StringBuilder userMessageText = new StringBuilder();
            
            // 视频文件处理策略：
            // 1. 如果文件较小（<20MB），可以尝试base64编码
            // 2. 如果文件较大，建议上传到OSS/CDN后使用URL
            // 3. 当前实现：对于小文件使用base64，大文件使用路径描述
            
            double fileSizeMB = videoFile.length() / 1024.0 / 1024.0;
            userMessageText.append("视频文件信息：\n");
            userMessageText.append("- 文件路径：").append(videoFile.getAbsolutePath()).append("\n");
            userMessageText.append("- 文件大小：").append(String.format("%.2f", fileSizeMB)).append(" MB\n\n");
            
            // 如果文件较小，尝试base64编码（注意：实际API可能不支持视频base64，需要根据API文档调整）
            if (fileSizeMB < 20) {
                try {
                    byte[] videoBytes = Files.readAllBytes(videoFile.toPath());
                    String videoBase64 = Base64.getEncoder().encodeToString(videoBytes);
                    userMessageText.append("视频文件（base64编码，前100字符）：").append(videoBase64.substring(0, Math.min(100, videoBase64.length()))).append("...\n\n");
                    log.info("视频文件已编码为base64，大小: {} MB", fileSizeMB);
                } catch (Exception e) {
                    log.warn("视频文件base64编码失败，使用路径描述", e);
                }
            } else {
                log.warn("视频文件较大（{} MB），建议上传到OSS/CDN后使用URL方式", fileSizeMB);
            }
            
            userMessageText.append("请分析这个视频，并按照要求返回JSON格式的分析结果。");

            return userMessageText.toString();

        } catch (Exception e) {
            log.error("构建用户消息时发生异常", e);
            throw new RuntimeException("构建用户消息失败: " + e.getMessage(), e);
        }
    }


}

