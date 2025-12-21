package com.xy.lucky.file.util;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

@Slf4j
public class WatermarkUtils {

    /**
     * 给图片添加文字水印并输出为流
     *
     * @param sourceImage   原始图片（本地路径、URL 或 InputStream）
     * @param watermarkText 水印文字
     * @param font          水印文字的字体
     * @param color         水印文字的颜色
     * @param position      水印位置
     * @param transparency  水印透明度 (0.0f ~ 1.0f)
     * @return 带文字水印的图片流
     * @throws Exception 异常
     */
    public static InputStream addTextWatermarkToStream(Object sourceImage, String watermarkText,
                                                       Font font, Color color, Positions position, String format, float transparency) throws Exception {
        BufferedImage sourceBufferedImage = readImage(sourceImage);

        // 创建文字水印
        BufferedImage watermark = createTextWatermark(watermarkText, font, color, sourceBufferedImage.getWidth(), sourceBufferedImage.getHeight(), position, 25);

        // 添加水印并输出到流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(sourceBufferedImage)
                .size(sourceBufferedImage.getWidth(), sourceBufferedImage.getHeight())
                .watermark(Positions.BOTTOM_RIGHT, watermark, transparency)
                .outputFormat(format)
                .toOutputStream(outputStream);

        log.info("Watermark created successfully");

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 给图片添加图片水印并输出为流
     *
     * @param sourceImage    原始图片（本地路径、URL 或 InputStream）
     * @param watermarkImage 水印图片（本地路径、URL 或 InputStream）
     * @param position       水印位置
     * @param transparency   水印透明度 (0.0f ~ 1.0f)
     * @return 带图片水印的图片流
     * @throws Exception 异常
     */
    public static InputStream addImageWatermarkToStream(Object sourceImage, Object watermarkImage,
                                                        Positions position, float transparency) throws Exception {
        BufferedImage sourceBufferedImage = readImage(sourceImage);
        BufferedImage watermarkBufferedImage = readImage(watermarkImage);

        // 添加水印并输出到流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(sourceBufferedImage)
                .size(sourceBufferedImage.getWidth(), sourceBufferedImage.getHeight())
                .watermark(position, watermarkBufferedImage, transparency)
                .toOutputStream(outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 读取图片（支持本地路径、网络 URL 和 InputStream）
     *
     * @param image 图片来源
     * @return BufferedImage
     * @throws Exception 异常
     */
    private static BufferedImage readImage(Object image) throws Exception {
        if (image instanceof String) {
            String imagePath = (String) image;
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                try (InputStream inputStream = new URL(imagePath).openStream()) {
                    return ImageIO.read(inputStream);
                }
            } else {
                return ImageIO.read(new File(imagePath));
            }
        } else if (image instanceof InputStream) {
            return ImageIO.read((InputStream) image);
        } else {
            throw new IllegalArgumentException("Unsupported image source type. Must be a String (path/URL) or InputStream.");
        }
    }

    /**
     * 创建文字水印图片
     *
     * @param text         水印文字
     * @param font         字体
     * @param color        颜色
     * @param imageWidth   图片宽度
     * @param imageHeight  图片高度
     * @param position     图片位置
     * @param edegDistance 边距
     * @return BufferedImage 水印图片
     */
    private static BufferedImage createTextWatermark(String text, Font font, Color color,
                                                     int imageWidth, int imageHeight, Positions position, Integer edegDistance) {
        // 创建与源图像相同大小的透明画布
        BufferedImage watermark = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermark.createGraphics();

        // 设置字体与颜色
        g2d.setFont(font);
        g2d.setColor(color);

        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 计算文字宽高
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();

        // 计算文字位置（根据指定的 Positions 参数）
        int x = 0, y = 0;
        switch (position) {
            case TOP_LEFT:
                x = edegDistance; // 边距
                y = textHeight + edegDistance;
                break;
            case TOP_CENTER:
                x = (imageWidth - textWidth) / 2;
                y = textHeight + edegDistance;
                break;
            case TOP_RIGHT:
                x = imageWidth - textWidth - edegDistance;
                y = textHeight + edegDistance;
                break;
            case CENTER:
                x = (imageWidth - textWidth) / 2;
                y = (imageHeight - textHeight) / 2 + fontMetrics.getAscent();
                break;
            case BOTTOM_LEFT:
                x = edegDistance;
                y = imageHeight - edegDistance;
                break;
            case BOTTOM_CENTER:
                x = (imageWidth - textWidth) / 2;
                y = imageHeight - edegDistance;
                break;
            case BOTTOM_RIGHT:
                x = imageWidth - textWidth - edegDistance;
                y = imageHeight - edegDistance;
                break;
            default:
                throw new IllegalArgumentException("Unsupported position: " + position);
        }

        // 绘制文字
        g2d.drawString(text, x, y);
        g2d.dispose();
        return watermark;
    }
}

