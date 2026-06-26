package com.agent.aicomposer.service;

import com.agent.aicomposer.model.entity.ArticleState;
import com.agent.aicomposer.model.enums.ImageMethodEnum;

public interface ImgSearchService {
//    String searchImg(String keywords);
    String searchImg(ArticleState.ImageRequirement imageRequirement);

    ImageMethodEnum getMethod();

    String getFullbackImg(String image_type);
}
