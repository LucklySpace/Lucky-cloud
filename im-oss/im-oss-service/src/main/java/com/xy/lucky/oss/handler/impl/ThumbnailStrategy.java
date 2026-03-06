package com.xy.lucky.oss.handler.impl;

import com.xy.lucky.oss.domain.OssFileMediaInfo;
import com.xy.lucky.oss.handler.ImageProcessingStrategy;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 缩略图处理策略
 */
@Slf4j
@Component("thumbnail")
public class ThumbnailStrategy implements ImageProcessingStrategy {

    @Override
    public InputStream process(InputStream inputStream, OssFileMediaInfo ossMediaFileInfo) throws Exception {
        // 验证图片是否有效
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        if (bufferedImage == null) {
            throw new IllegalArgumentException("无法读取图片，输入流可能不是有效的图片格式");
        }

        // 获取缩放比例与格式
        double ratio = ossMediaFileInfo.getRatio();
        String format = ossMediaFileInfo.getFormat();

        int width = (int) Math.round(bufferedImage.getWidth() * ratio);
        int height = (int) Math.round(bufferedImage.getHeight() * ratio);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 使用 Thumbnailator 生成缩略图并写入输出流
            Thumbnails.of(bufferedImage)
                    .size(width, height)
                    .outputQuality(0.8f)
                    .outputFormat(format)
                    .toOutputStream(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}
