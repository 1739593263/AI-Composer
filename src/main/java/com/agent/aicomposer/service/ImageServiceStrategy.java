package com.agent.aicomposer.service;

import com.agent.aicomposer.model.dto.image.ImageData;
import com.agent.aicomposer.model.dto.image.ImageRequest;
import com.agent.aicomposer.model.entity.ArticleState;
import com.agent.aicomposer.model.enums.ImageMethodEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ImageServiceStrategy {

    @Resource
    private List<ImgSearchService> imgSearchServices;

    private final Map<ImageMethodEnum, ImgSearchService> serviceMap = new EnumMap<>(ImageMethodEnum.class);

    @PostConstruct
    public void init() {
        // 将所有ImgService注册到映射列表
        for (ImgSearchService service : imgSearchServices) {
            ImageMethodEnum method = service.getMethod();
            serviceMap.put(method, service);
            log.info("注册图片检索服务：{} -> {} (AI生图: {}, 降级: {})",
                    method.getValue(),
                    service.getClass().getSimpleName(),
                    method.isAiGenerated(),
                    method.isFallback());
        }
    }

    public ImageResult getImageAndUpload(String imageSource, ImageRequest request) {
        ImageMethodEnum method = resolveMethod(imageSource);
        ImgSearchService service = serviceMap.get(method);

        if (service == null || !service.isAvailable()) {
            log.warn("图片服务不可用：{}，尝试降级", method);
            return handleFallbackWithUpload(request.getPosition());
        }

        try {
            // 1. 获取图片数据
            ImageData imageData = service.getImageData(request);

            if (imageData == null || !imageData.isValid()) {
                log.warn("图片数据获取失败，使用降级方案，method={}",method);
                return handleFallbackWithUpload(request.getPosition());
            }

            // 2. 上传到COS
            String folder = getFolderForMethod(method);
//            String cosUrl = cosService.uploadImageData(imageData, folder);


        } catch (Exception e) {
            log.error("获取图片并上传异常，method = {}", method, e);
            return handleFallbackWithUpload(request.getPosition());
        }
        return handleFallbackWithUpload(request.getPosition());
    }

    /**
     * 解析图片来源，处理未知值
     * @param imageSource
     * @return
     */
    private ImageMethodEnum resolveMethod(String imageSource) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(imageSource);
        if (method == null) {
            log.warn("未知的图片来源：{}, 默认使用 {}", imageSource, ImageMethodEnum.getDefaultSearchMethod());
            return ImageMethodEnum.getDefaultSearchMethod();
        }
        return method;
    }

    /**
     * 降级逻辑
     * @param position
     * @return
     */
    private ImageResult handleFallbackWithUpload(Integer position) {
        int pos = position != null ? position:1;
//        String fallbackUrl = getFallbackImage(pos);
        String fallbackUrl = "";

        // 上传降级图片到COS
        ImageData fallbackData = ImageData.fromDataUrl(fallbackUrl);
        // todo: 构建Cos服务接口逻辑
        String cosUrl = "";

        // 如果上传失败，直接使用原始URL
        String finalUrl = (cosUrl != null && !cosUrl.isEmpty()) ? cosUrl:fallbackUrl;
        return new ImageResult(finalUrl, ImageMethodEnum.getFallbackMethod());
    }

    public String getFallbackImage(int position) {
        ImgSearchService defaultService = serviceMap.get(ImageMethodEnum.getDefaultAiMethod());
        String image_type = position > 0?"inline":"cover";
        if (defaultService != null) {
            return defaultService.getFullbackImg(image_type);
        }
        // image_type 可能需要转换成 int形式
        return String.format("https://picsum.photos/800/600?random=%d", position);
    }

    private String getFolderForMethod(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "pexels";
            case NANO_BANANA -> "nano-banana";
            case MERMAID -> "mermaid";
            case ICONIFY -> "iconify";
            case EMOJI_PACK -> "emoji-pack";
            case SVG_DIAGRAM -> "svg-diagram";
            case PICSUM -> "picsum";
        };
    }

    public static class ImageResult {
        private final String url;
        private final ImageMethodEnum method;

        public ImageResult(String url, ImageMethodEnum method) {
            this.url = url;
            this.method = method;
        }

        public String getUrl() {
            return url;
        }

        public ImageMethodEnum getMethod() {
            return method;
        }

        public boolean isSuccess() {
            return url != null && !url.isEmpty();
        }
    }

}
