package com.xy.lucky.oss.handler.impl;


import com.xy.lucky.oss.domain.OssFileMediaInfo;
import com.xy.lucky.oss.handler.ImageProcessingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片压缩策略
 */
@Slf4j
@Component("compress")
public class CompressStrategy implements ImageProcessingStrategy {

    /**
     * 对图片进行原比例无损压缩，返回压缩后的 InputStream
     *
     * @param inputStream      输入图片流
     * @param ossMediaFileInfo 图片信息
     * @return 压缩后的 InputStream
     * @throws IOException 如果发生 IO 异常
     */
    public InputStream process(InputStream inputStream, OssFileMediaInfo ossMediaFileInfo) throws IOException {
        // 读取输入流中的图片
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IllegalArgumentException("输入流不是有效的图片");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 创建新的 BufferedImage，用于无损压缩
        BufferedImage compressedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = compressedImage.createGraphics();

        // 绘制原图到新的 BufferedImage
        g2d.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();

        // 将压缩后的图片写入 ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(compressedImage, "png", outputStream)) {
                throw new IOException("不支持的图片格式: " + "png");
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

}
