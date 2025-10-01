package com.xy.file.handler.impl;

import com.xy.file.domain.OssFileMediaInfo;
import com.xy.file.handler.ImageProcessingStrategy;
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
        // 将输入流包装成可重复使用的流
        ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            tempOutputStream.write(buffer, 0, bytesRead);
        }
        byte[] imageBytes = tempOutputStream.toByteArray();
        InputStream reusableInputStream = new ByteArrayInputStream(imageBytes);

        // 验证图片是否有效
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
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
            Thumbnails.of(reusableInputStream)
                    .size(width, height)
                    .outputQuality(0.8f)
                    .outputFormat(format)
                    .toOutputStream(outputStream);
            log.info("Thumbnails created successfully");
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}
