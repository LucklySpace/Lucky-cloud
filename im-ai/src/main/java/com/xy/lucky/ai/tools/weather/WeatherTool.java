package com.xy.lucky.ai.tools.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
public class WeatherTool {

    private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/forecast.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebClient webClient;


    @Tool(description = "使用 api.weather 获取天的预报和天气情况.")
    public String getWeatherServiceMethod(@ToolParam(description = "城市名称") String city,
                                          @ToolParam(description = "天气预报的天数。值的范围为1到14") int days) {

        if (!StringUtils.hasText(city)) {
            log.error("无效请求，必须传城市名称");
            return null;
        }
        String location = this.preprocessLocation(city);
        int safeDays = Math.max(1, Math.min(days, 14));
        if (webClient == null) {
            webClient = WebClient.create();
        }
        String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
                .queryParam("q", location)
                .queryParam("days", safeDays)
                .queryParam("key", System.getenv().getOrDefault("WEATHER_API_KEY", ""))
                .toUriString();

        log.info("url : {}", url);
        String result = "获取天气数据失败";
        try {
            result = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("获取天气数据成功: {}", result);
        } catch (Exception e) {
            log.error("获取天气数据失败: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 处理中文地名
     *
     * @param location
     * @return
     */
    public String preprocessLocation(String location) {
        if (containsChinese(location)) {
//            return PinyinUtil.getPinyin(location, "");
        }
        return location;
    }

    /**
     * 判断是否包含中文
     *
     * @param str
     * @return
     */
    public boolean containsChinese(String str) {
        return str.matches(".*[\u4e00-\u9fa5].*");
    }
}
