package com.xy.file.handler;


import com.xy.file.domain.OssFileMediaInfo;

import java.io.InputStream;

/**
 * 策略接口
 */
public interface ImageProcessingStrategy {

    InputStream process(InputStream inputStream, OssFileMediaInfo ossMediaFileInfo) throws Exception;
}
