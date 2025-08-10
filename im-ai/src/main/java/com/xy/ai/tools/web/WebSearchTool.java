//package com.xy.ai.tools.web;
//
//
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.execution.ToolCallResultConverter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.lang.annotation.Annotation;
//import java.util.List;
//import java.util.Map;
//
///**
// * 联网搜索工具类
// */
//@Component
//public class WebSearchTool implements Tool {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @Value("${bing.search.endpoint}")
//    private String bingEndpoint;
//
//    @Value("${bing.search.api-key}")
//    private String bingApiKey;
//
//    @Override
//    public String name() {
//        return "web_search";
//    }
//
//    @Override
//    public String description() {
//        return "在互联网上进行实时搜索，输入关键词，返回搜索结果摘要";
//    }
//
//    @Override
//    public boolean returnDirect() {
//        return false;
//    }
//
//    @Override
//    public Class<? extends ToolCallResultConverter> resultConverter() {
//        return null;
//    }
//
//    @Override
//    public Object run(Object input, CallbackContext context) {
//        String query = input.toString();
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Ocp-Apim-Subscription-Key", bingApiKey);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        UriComponentsBuilder builder = UriComponentsBuilder
//                .fromHttpUrl(bingEndpoint)
//                .queryParam("q", query)
//                .queryParam("count", 5);
//
//        HttpEntity<Void> entity = new HttpEntity<>(headers);
//        ResponseEntity<Map> resp = restTemplate.exchange(
//                builder.toUriString(),
//                HttpMethod.GET,
//                entity,
//                Map.class
//        );
//
//        // 简单提取 top 5 标题和摘要
//        List<Map<String, Object>> webPages = (List<Map<String, Object>>)
//                ((Map<?, ?>) resp.getBody().get("webPages")).get("value");
//        StringBuilder sb = new StringBuilder();
//        for (Map<String, Object> page : webPages) {
//            sb.append("- ").append(page.get("name")).append("：")
//                    .append(page.get("snippet")).append("\n  ")
//                    .append(page.get("url")).append("\n");
//        }
//        return sb.toString();
//    }
//
//    @Override
//    public Class<? extends Annotation> annotationType() {
//        return null;
//    }
//}
