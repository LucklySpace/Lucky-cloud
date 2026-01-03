package com.xy.lucky.lbs.service.impl;

import com.xy.lucky.lbs.exception.LbsException;
import com.xy.lucky.lbs.service.ThirdPartyMapService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class AmapServiceImpl implements ThirdPartyMapService {

    private final OkHttpClient client = new OkHttpClient();

    @Value("${amap.key:}")
    private String apiKey;

    /**
     * 获取地址信息
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 地址信息
     */
    @Override
    public String getAddress(double lat, double lon) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Unknown Address (Key missing)";
        }

        String url = "https://restapi.amap.com/v3/geocode/regeo?key=" + apiKey + "&location=" + lon + "," + lat;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // Parse JSON (Using Jackson or similar)
                // For now just return body string or truncated
                return response.body().string();
            }
        } catch (IOException e) {
            log.error("Failed to call Amap API", e);
        }
        throw new LbsException("高德api位置查询异常");
    }
}
