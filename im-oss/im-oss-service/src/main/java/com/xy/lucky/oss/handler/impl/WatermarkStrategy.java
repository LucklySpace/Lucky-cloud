package com.xy.lucky.oss.handler.impl;

import com.xy.lucky.oss.domain.OssFileMediaInfo;
import com.xy.lucky.oss.handler.ImageProcessingStrategy;
import com.xy.lucky.oss.util.WatermarkUtils;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;

/**
 * 图片水印策略实现
 */
@Component("watermark")
public class WatermarkStrategy implements ImageProcessingStrategy {

    @Value("${watermark.word}")
    private String watermarkWord;

    @Value("${watermark.font}")
    private String watermarkFont;

    @Override
    public InputStream process(InputStream inputStream, OssFileMediaInfo ossMediaFileInfo) throws Exception {

//        String watermarkPath = ossMediaFileInfo.getWatermarkPath();
//        Float opacity = ossMediaFileInfo.getOpacity();
//        Double scale = ossMediaFileInfo.getScale();

        String format = ossMediaFileInfo.getFormat();

        return WatermarkUtils.addTextWatermarkToStream(
                inputStream,
                watermarkWord,
                new Font(watermarkFont, Font.BOLD, 30),
                Color.lightGray,
                Positions.BOTTOM_RIGHT,
                format,
                0.5f
        );
    }
}
