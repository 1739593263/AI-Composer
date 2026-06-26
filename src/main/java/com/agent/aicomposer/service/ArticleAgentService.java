package com.agent.aicomposer.service;

import com.agent.aicomposer.constant.PromptConstant;
import com.agent.aicomposer.model.enums.ImageMethodEnum;
import com.agent.aicomposer.model.enums.SseMessageTypeEnum;
import com.agent.aicomposer.model.entity.ArticleState;
import com.agent.aicomposer.utils.GsonUtils;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.parser.Cons;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class ArticleAgentService {
    @Resource
    private DashScopeChatModel chatModel;

    @Resource
    private ImgSearchService imgSearchService;

    /**
     * 执行完整的文章生成流程
     *
     * @param state         文章状态
     * @param streamHandler 流式输出处理器
     */
    public void executeArticleGeneration(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 智能体1：生成标题
            log.info("智能体1：开始生成标题, taskId={}", state.getTaskId());
            agent1GenerateTitle(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT1_COMPLETE.getValue());

            // 智能体2：生成大纲（流式输出）
            log.info("智能体2：开始生成大纲, taskId={}", state.getTaskId());
            agent2GenerateOutline(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT2_COMPLETE.getValue());

            // 智能体3：生成正文（流式输出）
            log.info("智能体3：开始生成正文, taskId={}", state.getTaskId());
            agent3GenerateContent(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT3_COMPLETE.getValue());

            // 智能体4：分析配图需求
            log.info("智能体4：开始分析配图需求, taskId={}", state.getTaskId());
            agent4AnalyzeImageRequirements(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT4_COMPLETE.getValue());

            // 智能体5：生成配图
            log.info("智能体5：开始生成配图, taskId={}", state.getTaskId());
            agent5GenerateImages(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT5_COMPLETE.getValue());

            // 图文合成：将配图插入正文
            log.info("开始图文合成, taskId={}", state.getTaskId());
            mergeImagesIntoContent(state);
            streamHandler.accept(SseMessageTypeEnum.MERGE_COMPLETE.getValue());

            log.info("文章生成完成, taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.error("文章生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("文章生成失败: " + e.getMessage(), e);
        }
    }

    private void agent1GenerateTitle(ArticleState state) {
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", state.getTopic());

        String result = callLlm(prompt);
        ArticleState.TitleResult titleResult = parseJsonResponse(result, ArticleState.TitleResult.class, "标题");
        state.setTitle(titleResult);
        log.info("智能体1: 标题生成成功, titleResult={}",titleResult.getMain_title());
    }

    private void agent2GenerateOutline(ArticleState state, Consumer<String> streamHandler) {
        String prompt = PromptConstant.AGENT2_OUTLINE_PROMPT
                .replace("{mainTitle}", state.getTitle().getMain_title())
                .replace("{subTitle}", state.getTitle().getSub_title());
        String result = callLlmWithStreaming(prompt, streamHandler, SseMessageTypeEnum.AGENT2_STREAMING);
        ArticleState.OutlineResult outlineResult = parseJsonResponse(result, ArticleState.OutlineResult.class, "大纲");
        state.setOutline(outlineResult);
        log.info("智能体2: 大纲生成成功, sections={}",outlineResult.getSections().size());
    }

    private void agent3GenerateContent(ArticleState state, Consumer<String> streamHandler) {
        String outlineText = GsonUtils.toJson(state.getOutline().getSections());
        String prompt = PromptConstant.AGENT3_CONTENT_PROMPT
                .replace("{mainTitle}", state.getTitle().getMain_title())
                .replace("{subTitle}", state.getTitle().getSub_title())
                .replace("{outline}", outlineText);
        String result = callLlmWithStreaming(prompt, streamHandler, SseMessageTypeEnum.AGENT3_STREAMING);
        state.setContent(result);
        log.info("智能体3: 正文生成成功, length={}",result.length());
    }

    private void agent4AnalyzeImageRequirements(ArticleState state) {
        String sectionTitleText = state.getOutline().getSections().stream().map(section -> section.getSection_title()).toString();
        String prompt = PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT
                .replace("{mainTitle}", state.getTitle().getMain_title())
                .replace("{articleContent}", state.getContent())
                .replace("{sectionTitles}", sectionTitleText);

        String result = callLlm(prompt);
        List<ArticleState.ImageRequirement> imageRequirements = parseJsonListResponse(result,
                new TypeToken<List<ArticleState.ImageRequirement>>() {
                },
                "配图需求");
        state.setImageRequirements(imageRequirements);
        log.info("智能体4: 配图需求分析成功, count={}",imageRequirements.size());
    }

    public void agent5GenerateImages(ArticleState state, Consumer<String> streamHandler) {
        List<ArticleState.ImageResult> imageResults = new ArrayList<>();
        List<ArticleState.ImageRequirement> imageRequirements = state.getImageRequirements();

        for (ArticleState.ImageRequirement requirement:imageRequirements) {
            log.info("智能体5：开始检索配图, type={}, keyword={}", requirement.getImage_type(), requirement.getMain_keyword());

            // 1. search images
//            String imgUrl = imgSearchService.searchImg(requirement.getMain_keyword());
            String imgUrl = imgSearchService.searchImg(requirement);

            // 2. 降级策略
            ImageMethodEnum method = imgSearchService.getMethod();
            if (imgUrl == null) {
                imgUrl = imgSearchService.getFullbackImg(requirement.getImage_type());
                method = ImageMethodEnum.PICSUM; // AI生成随机图片
                log.info("智能体5：检索图片失败，使用降级方案, type={}", requirement.getImage_type());
            }
            // 3. 获取云服务图片地址（当前不实现上传COS腾讯云）
            imgUrl = imgUrl;

            // 4. 构建ImageResult
            ArticleState.ImageResult imageResult = buildImageResult(requirement, imgUrl, method);
            imageResults.add(imageResult);

            // 5. 推送完成配置的单图
            String imageCompleteMsg = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                    + GsonUtils.toJson(imageResult);
            streamHandler.accept(imageCompleteMsg);

            log.info("智能体5：检索配图成功, type={}, keyword={}", requirement.getImage_type(), requirement.getMain_keyword());
        }

        state.setImages(imageResults);
        log.info("智能体5：所有检索配图完成, count={}", imageResults.size());
    }

    public void mergeImagesIntoContent(ArticleState state) {
        String content = state.getContent();
        List<ArticleState.ImageResult> images = state.getImages();
        if (images == null || images.size()==0) {
            state.setFullContent(content);
            return;
        }

        StringBuilder fullContentBuilder = new StringBuilder();
        String[] lines = content.split("\n");
        for (String line : lines) {
            fullContentBuilder.append(line).append("\n");
            if (line.startsWith("## ")) {
                String sectionTitle = line.substring(3).trim();
                insertImageAfterSection(fullContentBuilder, images, sectionTitle);
            }
        }

        state.setFullContent(fullContentBuilder.toString());
        log.info("图文合成完成, fullContentLength={}",fullContentBuilder.length());
    }

    /**
     * 非流式调用LLM
     * @param prompt
     * @return
     */
    private String callLlm(String prompt) {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult().getOutput().getText();
    }

    private String callLlmWithStreaming(String prompt, Consumer<String> streamHandler, SseMessageTypeEnum messageTypeEnum) {
        Flux<ChatResponse> stream = chatModel.stream(new Prompt(new UserMessage(prompt)));
        StringBuilder contentBuilder = new StringBuilder();
        stream.doOnNext(response -> {
            String chunk = response.getResult().getOutput().getText();
            if (chunk!=null || !chunk.isEmpty()) {
                contentBuilder.append(chunk);
                streamHandler.accept(messageTypeEnum.getStreamingPrefix()+chunk);
            }
        })
                .doOnError(error->log.error("LLM调用失败， MessageType={}",messageTypeEnum,error))
                .blockLast();
        return contentBuilder.toString();
    }

    private <T> T parseJsonResponse(String content, Class<T> clazz, String name) {
        try {
            return GsonUtils.fromJson(content, clazz);
        } catch (JsonSyntaxException e) {
            log.error("{}解析失败, content={}", name, content, e);
            throw new RuntimeException(name + "解析失败");
        }
    }

    private <T> T parseJsonListResponse(String content, TypeToken<T> typeToken, String name) {
        try {
            return GsonUtils.fromJson(content, typeToken);
        } catch (JsonSyntaxException e) {
            log.error("{}解析失败, content={}", name, content, e);
            throw new RuntimeException(name + "解析失败");
        }
    }

    private ArticleState.ImageResult buildImageResult(
            ArticleState.ImageRequirement requirement,
            String imageUrl,
            ImageMethodEnum method) {
        ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
        imageResult.setImage_type(requirement.getImage_type());
        imageResult.setUrl(imageUrl);
        imageResult.setMethod(method.getValue());
        imageResult.setKeywords(requirement.getMain_keyword());
        imageResult.setSectionTitle(requirement.getSection_title());
        imageResult.setDescription(requirement.getVisual_prompt());
        return imageResult;
    }

    private void insertImageAfterSection(StringBuilder fullContent,
                                         List<ArticleState.ImageResult> imageResults,
                                         String sectionTitle) {
        for (ArticleState.ImageResult imageResult : imageResults) {
            if (imageResult.getImage_type().equals("inline") &&
                    imageResult.getSectionTitle() != null &&
                    sectionTitle.contains(imageResult.getSectionTitle().trim())) {
                fullContent.append("\n![").append(imageResult.getDescription())
                        .append("](").append(imageResult.getUrl()).append(")\n");
                break;
            }
        }
    }
}
