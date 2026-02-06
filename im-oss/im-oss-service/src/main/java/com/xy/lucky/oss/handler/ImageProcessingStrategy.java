package com.xy.lucky.oss.handler;


import com.xy.lucky.oss.domain.OssFileMediaInfo;

import java.io.InputStream;

/**
 * 策略接口
 */
public interface ImageProcessingStrategy {

    InputStream process(InputStream inputStream, OssFileMediaInfo ossMediaFileInfo) throws Exception;
}
