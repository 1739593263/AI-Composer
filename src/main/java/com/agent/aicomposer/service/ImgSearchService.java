package com.agent.aicomposer.service;

import com.agent.aicomposer.model.dto.image.ImageData;
import com.agent.aicomposer.model.dto.image.ImageRequest;
import com.agent.aicomposer.model.entity.ArticleState;
import com.agent.aicomposer.model.enums.ImageMethodEnum;

public interface ImgSearchService {
    default String getImage(ImageRequest request) {
        String param = request.getEffectiveParam(getMethod().isAiGenerated());
        return searchImg(param);
    }

    /**
     * 获取图片数据 （用于上传COS）
     *
     * @param request
     * @return
     */
    default ImageData getImageData(ImageRequest request) {
        String url = getImage(request);
        return ImageData.fromUrl(url);
    }

    /**
     * 仅根据关键词获取图片
     * @param keywords
     * @return
     */
    String searchImg(String keywords);

    /**
     * 根据完整图片需求获取图片
     * @param imageRequirement
     * @return
     */
    String searchImg(ArticleState.ImageRequirement imageRequirement);

    ImageMethodEnum getMethod();

    /**
     * 获取降级图片
     *
     * @param position
     * @return
     */
    String getFullbackImg(int position);

    /**
     * 获取降级图片(根据类型['inline','cover']获取)
     *
     * @param image_type
     * @return
     */
    String getFullbackImg(String image_type);

    /**
     * 判断服务是否可用
     *
     * @return
     */
    default boolean isAvailable() {
        return true;
    }
}
