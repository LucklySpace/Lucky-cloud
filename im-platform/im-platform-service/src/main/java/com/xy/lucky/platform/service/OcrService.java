package com.xy.lucky.platform.service;

import com.xy.lucky.platform.config.PaddleOcrProperties;
import com.xy.lucky.platform.domain.vo.OcrResponseVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ocr 远程调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final PaddleOcrProperties props;

    /**
     * OCR 专用 WebClient
     * - 统一超时
     * - 避免频繁创建连接
     */
    private WebClient ocrClient;

    /**
     * 下载图片专用 WebClient
     */
    private WebClient downloadClient;

    /**
     * 懒加载初始化 WebClient
     */
    private void initClientsIfNeeded() {
        if (ocrClient != null) {
            return;
        }

        long timeout = Math.max(2000, props.getTimeoutMillis());
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeout));

        ReactorClientHttpConnector connector =
                new ReactorClientHttpConnector(httpClient);

        this.ocrClient = WebClient.builder()
                .clientConnector(connector)
                .build();

        this.downloadClient = WebClient.builder()
                .clientConnector(connector)
                .build();
    }

    /* ======================= 对外 API ======================= */

    /**
     * 识别 Base64 图片
     */
    public OcrResponseVo recognizeBase64(List<String> images) {
        ensureEnabled();
        validateNotEmpty(images, "images");
        return postImages(images);
    }

    /**
     * 识别字节数组图片
     */
    public OcrResponseVo recognizeBytes(byte[] data) {
        ensureEnabled();
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("图像数据为空");
        }
        return recognizeBase64(List.of(toBase64(data)));
    }

    /**
     * 识别单个 URL 图片
     */
    public OcrResponseVo recognizeUrl(String url) {
        ensureEnabled();
        return recognizeUrls(List.of(url));
    }

    /**
     * 识别多个 URL 图片
     */
    public OcrResponseVo recognizeUrls(List<String> urls) {
        ensureEnabled();
        validateNotEmpty(urls, "urls");

        List<String> images = urls.stream()
                .filter(Objects::nonNull)
                .map(this::fetchBytes)
                .map(this::toBase64)
                .toList();

        return postImages(images);
    }

    /* ======================= 核心逻辑 ======================= */

    /**
     * 向 PaddleOCR 服务提交图片数据
     */
    private OcrResponseVo postImages(List<String> images) {
        initClientsIfNeeded();

        String url = buildPredictUrl();
        Map<String, Object> payload = Map.of("images", images);

        return ocrClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OcrResponseVo.class)
                .block(requestTimeout());
    }

    /**
     * 下载远程图片为 byte[]
     */
    private byte[] fetchBytes(String url) {
        initClientsIfNeeded();
        validateHttpUrl(url);

        return downloadClient.get()
                .uri(url)
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(requestTimeout());
    }

    /* ======================= 工具方法 ======================= */

    private void ensureEnabled() {
        if (!props.isEnabled()) {
            throw new IllegalStateException("OCR 服务未启用");
        }
    }

    /**
     * 构建 OCR 预测接口地址
     */
    private String buildPredictUrl() {
        if (!StringUtils.hasText(props.getBaseUrl())
                || !StringUtils.hasText(props.getPredictPath())) {
            throw new IllegalStateException("PaddleOCR 服务地址未配置");
        }
        return trimSlash(props.getBaseUrl()) + props.getPredictPath();
    }

    /**
     * 统一请求超时
     */
    private Duration requestTimeout() {
        return Duration.ofMillis(
                Math.max(1000, props.getTimeoutMillis() + 2000)
        );
    }

    private String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void validateNotEmpty(List<?> list, String name) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private void validateHttpUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("url 不能为空");
        }
        String lower = url.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new IllegalArgumentException("仅支持 http/https URL");
        }
    }

    private String trimSlash(String base) {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}

