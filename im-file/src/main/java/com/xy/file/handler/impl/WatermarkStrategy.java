package com.xy.file.handler.impl;

import com.xy.file.entity.OssFileMediaInfo;
import com.xy.file.handler.ImageProcessingStrategy;
import com.xy.file.util.WatermarkUtil;
import net.coobird.thumbnailator.geometry.Positions;

import java.awt.*;
import java.io.InputStream;

/**
 * 图片水印策略实现
 */
public class WatermarkStrategy implements ImageProcessingStrategy {


    @Override
    public InputStream process(InputStream inputStream, OssFileMediaInfo ossMediaFileInfo) throws Exception {

//        String watermarkPath = ossMediaFileInfo.getWatermarkPath();
//        Float opacity = ossMediaFileInfo.getOpacity();
//        Double scale = ossMediaFileInfo.getScale();

        String format = ossMediaFileInfo.getFormat();

        return WatermarkUtil.addTextWatermarkToStream(
                inputStream,
                "Lynk",
                new Font("Arial", Font.BOLD, 30),
                Color.lightGray,
                Positions.BOTTOM_RIGHT,
                format,
                0.5f
        );
    }
}
