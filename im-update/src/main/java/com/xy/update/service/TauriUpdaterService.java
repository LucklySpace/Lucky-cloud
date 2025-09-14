package com.xy.update.service;

import com.xy.update.config.TauriUpdaterProperties;
import com.xy.update.domain.tauri.TauriUpdaterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service 层：封装版本信息与文件读取逻辑
 * <p>
 * - 负责：读取配置，查找文件，做路径规范化以防路径穿越
 * - 返回 Spring Resource（Controller 将直接返回 ResponseEntity）
 */
@Slf4j
@Service
public class TauriUpdaterService {

    // 优先使用环境变量/系统属性以便在不同环境下配置文件目录（便于 CI/CD / 容器化）
    private static final String DEFAULT_FILE_DIRECTORY = "D:\\Project\\im-client\\src-tauri\\target\\debug\\bundle\\msi\\";

    @Autowired
    private TauriUpdaterProperties props;

    /**
     * 返回最新版本信息（从配置 props 中读取）
     *
     * @param platform 平台标识，例如 windows-x86_64
     * @return TauriUpdaterResponse DTO（简洁封装）
     */
    public TauriUpdaterResponse latest(String platform) {
        log.debug("请求 latest, platform={}", platform);
        // 直接从配置读取（props 需要实现对应访问方法，如 version()/notes()/pubDate() 等）
        return new TauriUpdaterResponse(
                props.version(),
                props.notes(),
                props.pubDate(),
                // props.platforms() 或者你在 properties 中的获取方式
                props.getPlatformInfo(platform) // 假设 props 提供安全的 platformInfo 方法，返回 null 或对象
        );
    }

    /**
     * 下载文件（流式返回 Resource）
     * <p>
     * - 对 fileName 做严格校验与规范化，防止路径穿越
     * - 自动探测 Content-Type，设置 Content-Disposition（包含 UTF-8 编码的 filename*）
     *
     * @param fileName 要下载的文件名（仅允许文件名，不建议包含路径分隔符）
     * @return ResponseEntity<Resource> 或 对应错误状态
     */
    public ResponseEntity<Resource> downloadFile(String fileName) {
        log.info("downloadFile called, fileName={}", fileName);

        // 基本输入校验
        if (!StringUtils.hasText(fileName)) {
            log.warn("downloadFile: empty fileName");
            return ResponseEntity.badRequest().build();
        }

        // 禁止包含明显的路径穿越或绝对路径字符（额外约束）
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || fileName.contains(":/")) {
            log.warn("downloadFile: invalid fileName (possible path traversal) - {}", fileName);
            return ResponseEntity.status(403).build();
        }

        // 解析最终文件目录：优先读取 env/systemProperty，其次回退到默认常量
        String configured = System.getenv("UPDATER_FILE_DIR");
        if (!StringUtils.hasText(configured)) {
            configured = System.getProperty("updater.fileDirectory");
        }
        final String baseDir = StringUtils.hasText(configured) ? configured : DEFAULT_FILE_DIRECTORY;
        final Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        log.debug("Using base directory: {}", basePath);

        // 拼接并规范化请求的文件路径
        Path resolved;
        try {
            resolved = basePath.resolve(fileName).normalize();
        } catch (InvalidPathException ex) {
            log.warn("downloadFile: invalid path for fileName={}, ex={}", fileName, ex.getMessage());
            return ResponseEntity.badRequest().build();
        }

        // 再次校验：确保最终路径仍然在 basePath 下，防止越权
        if (!resolved.startsWith(basePath)) {
            log.warn("downloadFile: resolved path outside base directory. resolved={}, base={}", resolved, basePath);
            return ResponseEntity.status(403).build();
        }

        // 检查文件存在性与可读性
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
            log.info("downloadFile: file not found or not readable: {}", resolved);
            return ResponseEntity.notFound().build();
        }

        // 将文件包装为 Resource 并设置响应头
        try {
            Resource resource = new UrlResource(resolved.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("downloadFile: resource not readable after UrlResource creation: {}", resolved);
                return ResponseEntity.notFound().build();
            }

            // 尝试探测内容类型（若探测失败回退为二进制流）
            String contentType = null;
            try {
                contentType = Files.probeContentType(resolved);
            } catch (IOException e) {
                log.debug("probeContentType failed for {}, will use application/octet-stream", resolved, e);
            }
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            // 处理文件名（避免中文/特殊字符问题） - 使用 RFC5987 的 filename* 编码
            String filename = resolved.getFileName().toString();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename;

            log.info("downloadFile: serving file={}, contentType={}", resolved, contentType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(resolved)))
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("downloadFile: malformed url for path={}, ex={}", resolved, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        } catch (IOException e) {
            log.error("downloadFile: io error while serving file={}, ex={}", resolved, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
