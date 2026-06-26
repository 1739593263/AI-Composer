package com.agent.aicomposer.service;

import cn.hutool.json.JSONObject;
import com.agent.aicomposer.config.PexelsConfig;
import com.agent.aicomposer.model.entity.ArticleState;
import com.agent.aicomposer.model.enums.ImageMethodEnum;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class PexelsImgService implements ImgSearchService{

    @Resource
    private PexelsConfig pexelsConfig;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String searchImg(ArticleState.ImageRequirement imageRequirement) {
        try{
            String url = buildCompleteUrl(imageRequirement.getMain_keyword(), imageRequirement.getOrientation());
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", pexelsConfig.getAPIKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Pexels API 调用失败：{}",response.code());
                    return null;
                }

                String responseBody = response.body().string();
                return extractImageUrl(responseBody, imageRequirement.getMain_keyword());
            }
        } catch (IOException e) {
            log.error("Pexels API 调用异常", e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.PEXELS;
    }

    @Override
    public String getFullbackImg(String image_type) {
        return null;
    }

    /**
     * 构建完整搜索链接
     *
     * @param keywords
     * @param orientation
     * @return
     */
    private String buildCompleteUrl(String keywords, String orientation) {
        return String.format("%s?query=%s&per_page=%d&orientation=%s",
                pexelsConfig.getUrl(), keywords, 40, orientation);
    }

    private String extractImageUrl(String responseBody, String keywords) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray photos = jsonObject.getAsJsonArray("photos");

        if (photos.isEmpty()) {
            log.warn("Pexels 未检索到图片： {}", keywords);
            return null;
        }
        JsonObject photo = photos.get(0).getAsJsonObject();
        JsonObject src = photo.getAsJsonObject("src");
        return src.get("large").getAsString();
    }
}
