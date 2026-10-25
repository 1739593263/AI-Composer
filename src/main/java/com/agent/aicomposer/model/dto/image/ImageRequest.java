package com.agent.aicomposer.model.dto.image;

import lombok.Builder;
import lombok.Data;

/**
 * 图片请求对象：封装图片请求所需的各种参数
 */
@Data
@Builder
public class ImageRequest {
    /**
     * 图库检索关键词
     */
    private String keywords;
    private String prompt;
    private Integer position;
    /**
     * 图片类型：cover & section
     */
    private String type;
    /**
     * 宽高比
     */
    private String aspectRatio;
    private String style;

    public String getEffectiveParam(boolean isAiGenerated) {
        if (isAiGenerated) {
            return prompt != null && !prompt.isEmpty()? prompt : keywords;
        }
        return keywords != null && !keywords.isEmpty() ? keywords : prompt;
    }
}
