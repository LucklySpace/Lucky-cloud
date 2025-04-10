package com.xy.auth.service;

import jakarta.servlet.http.HttpServletResponse;


public interface QrCodeService {

    public String createCodeToBase64(String content);

    public void createCodeToStream(String content, HttpServletResponse response);
}