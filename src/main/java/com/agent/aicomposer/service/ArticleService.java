package com.agent.aicomposer.service;

import com.agent.aicomposer.model.dto.article.ArticleQueryRequest;
import com.agent.aicomposer.model.entity.Article;
import com.agent.aicomposer.model.entity.ArticleState;
import com.agent.aicomposer.model.entity.User;
import com.agent.aicomposer.model.enums.ArticleStatusEnum;
import com.agent.aicomposer.model.vo.ArticleVO;
import com.mybatisflex.core.paginate.Page;

public interface ArticleService {
    /**
     * 创建文章项目
     * @param topic
     * @param loginUser
     * @return
     */
    String createArticleTask(String topic, User loginUser);

    /**
     * 根据taskId获取文章项目
     * @param taskId
     * @return
     */
    Article getByTaskId(String taskId);

    /**
     * 更改流程中文章项目状态
     * @param taskId
     * @param status
     * @param errorMessage
     */
    void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage);

    /**
     * 保存文章内容
     * @param taskId
     * @param state
     */
    void saveArticleContent(String taskId, ArticleState state);

    /**
     * 获取文章项目细节
     * @param taskId
     * @param loginUser
     * @return
     */
    ArticleVO getArticleDetail(String taskId, User loginUser);

    /**
     * 分页查询
     * @param request
     * @param loginUser
     * @return
     */
    Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser);

    /**
     * 删除文章项目
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteArticle(Long id, User loginUser);
}
