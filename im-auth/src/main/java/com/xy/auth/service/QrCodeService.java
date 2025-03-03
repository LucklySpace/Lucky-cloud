package com.xy.auth.service;

import cn.hutool.extra.qrcode.QrCodeException;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class QrCodeService {

    @Resource
    private QrConfig config;

    //生成到文件
    public String createCodeToBase64(String content) {
        try {
            // 生成 Base64 二维码
            return QrCodeUtil.generateAsBase64(content, config, QrCodeUtil.QR_TYPE_SVG);
        } catch (QrCodeException e) {
            e.printStackTrace();
        }
        return null;
    }

    //生成到流
    public void createCodeToStream(String content, HttpServletResponse response) {
        try {
            QrCodeUtil.generate(content, config, "png", response.getOutputStream());
        } catch (QrCodeException | IOException e) {
            e.printStackTrace();
        }
    }
}