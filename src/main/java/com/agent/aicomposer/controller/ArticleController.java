package com.agent.aicomposer.controller;

import com.agent.aicomposer.aop.annotation.AuthCheck;
import com.agent.aicomposer.common.BaseResponse;
import com.agent.aicomposer.common.DeleteRequest;
import com.agent.aicomposer.common.ResultUtils;
import com.agent.aicomposer.exception.ErrorCode;
import com.agent.aicomposer.exception.ThrowUtils;
import com.agent.aicomposer.manager.SseEmitterManager;
import com.agent.aicomposer.model.dto.article.ArticleCreateRequest;
import com.agent.aicomposer.model.dto.article.ArticleQueryRequest;
import com.agent.aicomposer.model.entity.User;
import com.agent.aicomposer.model.vo.ArticleVO;
import com.agent.aicomposer.service.ArticleAsyncService;
import com.agent.aicomposer.service.ArticleService;
import com.agent.aicomposer.service.UserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("article")
@Tag(name = "文章窗口")
@Slf4j
public class ArticleController {
    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleAsyncService articleAsyncService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;


    /**
     * 创建文章任务
     */
    @PostMapping("/create")
    @Operation(summary = "创建文章任务")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "选题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 创建文章任务
        String taskId = articleService.createArticleTask(request.getTopic(), loginUser);

        // 异步执行文章生成
        articleAsyncService.executeArticleGeneration(taskId, request.getTopic());

        return ResultUtils.success(taskId);
    }

    /**
     * SSE 进度推送接口
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取文章生成进度")
    public SseEmitter getProgress(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        // 权限校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId, loginUser);
        // 创建emitter
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        log.info("SSE 连接建立, taskId = {}",taskId);

        return emitter;
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取文章详情")
    @AuthCheck(mustRole = "user")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        ArticleVO articleDetail = articleService.getArticleDetail(taskId, loginUser);

        return ResultUtils.success(articleDetail);
    }

    /**
     * 分页查询文章详情
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询文章列表")
    @AuthCheck(mustRole = "user")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request,
                                                     HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<ArticleVO> articleVOPage = articleService.listArticleByPage(request, loginUser);

        return ResultUtils.success(articleVOPage);
    }

    /**
     * 删除文章
     */
    @PostMapping ("/delete")
    @Operation(summary = "删除文章记录")
    @AuthCheck(mustRole = "user")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean deleteArticle = articleService.deleteArticle(deleteRequest.getId(), loginUser);
        return ResultUtils.success(deleteArticle);
    }
}

