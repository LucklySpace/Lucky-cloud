package com.xy.lucky.dubbo.api.platform;

public interface ImShortLinkDubboService {

    String createShortLink(String originalUrl);

    String expandShortLink(String shortCode);
}
