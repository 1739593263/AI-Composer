package com.agent.aicomposer.model.dto.image;

import lombok.Builder;
import lombok.Data;

import java.util.Base64;
import java.util.Locale;

/**
 * 图片数据封装类
 */
@Builder
@Data
public class ImageData {
    /**
     * 图片字节数据
     */
    private byte[] bytes;

    private String url;

    /**
     * MIME类型 （如image/png, image/jpeg, image/svg+xml）
     */
    private String mimeType;

    /**
     * 数据类型
     */
    private DataType dataType;

    /**
     * 数据类型枚举
     */
    public enum DataType {
        /**
         * 字节数据
         */
        BYTES,
        /**
         * 外部URL
         */
        URL,
        /**
         * base64 data URL
         */
        DATA_URL
    }

    public static ImageData fromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // 判断是否为 base64 data URL， 如果是，就根据base64 data URL创建ImageData对象
        if (url.startsWith("data:")) {
            return fromDataUrl(url);
        }

        return ImageData.builder()
                .url(url)
                .dataType(DataType.URL)
                .build();
    }

    /**
     * 根据 base64 Data URL 创建 ImageData
     * @param dataUrl
     * @return
     */
    public static ImageData fromDataUrl(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            return null;
        }

        // data URL 格式解析： data:image/png;base64,xxx
        String mimeType = "image/png";
        int mimeEnd = dataUrl.indexOf(";");
        if (mimeEnd > 5) {
            mimeType = dataUrl.substring(5, mimeEnd);
        }

        return ImageData.builder()
                .url(dataUrl)
                .mimeType(mimeType)
                .dataType(DataType.DATA_URL)
                .build();
    }

    /**
     * 根据字节数据创建 ImageData
     * @param bytes
     * @param mimeType
     * @return
     */
    public static ImageData fromBytes(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        return ImageData.builder()
                .bytes(bytes)
                .mimeType(mimeType != null ? mimeType:"image/png")
                .dataType(DataType.BYTES)
                .build();
    }

    /**
     * 获取图片字节数据
     * @return
     */
    public byte[] getImageBytes() {
        if (dataType == DataType.BYTES) {
            return bytes;
        }

        // data URL 解码base64 返回
        if (dataType == DataType.DATA_URL && url != null) {
            int base64Start = url.indexOf(",");
            if (base64Start > 0) {
                String base64Data = url.substring(base64Start + 1);
                return Base64.getDecoder().decode(base64Data);
            }
        }
        return null;
    }

    public boolean isValid() {
        return switch (dataType) {
            case BYTES -> bytes != null && bytes.length > 0;
            case URL, DATA_URL -> url != null && !url.isEmpty();
        };
    }

    public String getFileExtension() {
        if (mimeType == null) return ".png";

        return switch (mimeType.toLowerCase()) {
          case "image/jpeg","image.jpg" -> ".jpg";
          case "image/png" -> ".png";
          case "image/gif" -> ".gif";
          case "image/webp" -> ".webp";
          case "image/svg+xml" -> ".svg";
          default -> ".png";
        };
    }
}
