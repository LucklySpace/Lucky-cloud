package com.xy.lucky.platform.service;

import com.xy.lucky.platform.config.MinioProperties;
import com.xy.lucky.platform.domain.po.LanguagePackPo;
import com.xy.lucky.platform.domain.vo.LanguagePackVo;
import com.xy.lucky.platform.exception.LanguagePackException;
import com.xy.lucky.platform.mapper.LanguagePackVoMapper;
import com.xy.lucky.platform.repository.LanguagePackRepository;
import com.xy.lucky.platform.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 语言包管理服务
 * - 新增/修改语言包元信息
 * - 上传/下载语言包文件
 * - 文件存储于 MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageService {

    private final LanguagePackRepository repository;
    private final LanguagePackVoMapper mapper;
    private final MinioUtils minioUtils;
    private final MinioProperties minioProperties;

    /**
     * 新增或修改语言包元信息（不涉及文件）
     */
    @Transactional
    public LanguagePackVo upsert(LanguagePackVo request) {
        String locale = Optional.ofNullable(request.getLocale()).map(String::trim)
                .orElseThrow(() -> new LanguagePackException("locale 不能为空"));
        String name = Optional.ofNullable(request.getName()).map(String::trim)
                .orElseThrow(() -> new LanguagePackException("name 不能为空"));
        String version = Optional.ofNullable(request.getVersion()).map(String::trim)
                .orElseThrow(() -> new LanguagePackException("version 不能为空"));

        LanguagePackPo po = repository.findByLocale(locale)
                .orElse(LanguagePackPo.builder()
                        .locale(locale)
                        .build());
        po.setName(name);
        po.setVersion(version);
        po.setAuthor(request.getAuthor());
        po.setDescription(request.getDescription());
        LanguagePackPo saved = repository.save(po);
        return mapper.toVo(saved);
    }

    /**
     * 列出所有语言包
     */
    public List<LanguagePackVo> listAll() {
        return repository.findAll().stream()
                .map(mapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 上传或更新语言包文件（JSON或压缩包）
     * - 若语言包不存在则创建
     * - 文件上传至 MinIO 并更新下载地址与大小
     */
    @Transactional
    public LanguagePackVo upload(LanguagePackVo meta, FilePart file) {
        String locale = Optional.ofNullable(meta.getLocale()).map(String::trim)
                .orElseThrow(() -> new LanguagePackException("locale 不能为空"));

        if (file == null || !StringUtils.hasText(file.filename())) {
            throw new LanguagePackException("文件不能为空");
        }

        LanguagePackPo po = repository.findByLocale(locale).orElse(LanguagePackPo.builder()
                .locale(locale)
                .name(Optional.ofNullable(meta.getName()).orElse(locale))
                .version(Optional.ofNullable(meta.getVersion()).orElse("1.0.0"))
                .author(meta.getAuthor())
                .description(meta.getDescription())
                .build());

        String bucket = minioProperties.getBucketName();
        String filename = Optional.of(file.filename()).filter(StringUtils::hasText).orElse(locale + ".json");
        String objectName = minioUtils.getObjectName(filename);
        String objectKey = String.format("i18n/%s/%s/%s",
                po.getLocale(),
                Optional.ofNullable(meta.getVersion()).orElse(po.getVersion()),
                objectName);

        String contentType = file.headers() != null && file.headers().getContentType() != null
                ? file.headers().getContentType().toString() : "application/octet-stream";

        Path temp;
        long size;
        try {
            temp = Files.createTempFile("lang-", "-" + filename);
            file.transferTo(temp.toFile()).block();
            size = Files.size(temp);
        } catch (Exception e) {
            throw new LanguagePackException("文件接收失败: " + e.getMessage());
        }

        try {
            try (InputStream in = Files.newInputStream(temp)) {
                minioUtils.uploadObject(bucket, objectKey, in, size, contentType);
            }
        } catch (Exception e) {
            throw new LanguagePackException("文件上传失败: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignore) {
                log.warn("删除临时文件失败: {}", temp);
            }
        }

        String url = minioUtils.presignedGetUrl(bucket, objectKey, 24 * 60 * 60);

        po.setName(Optional.ofNullable(meta.getName()).orElse(po.getName()));
        po.setVersion(Optional.ofNullable(meta.getVersion()).orElse(po.getVersion()));
        po.setAuthor(Optional.ofNullable(meta.getAuthor()).orElse(po.getAuthor()));
        po.setDescription(Optional.ofNullable(meta.getDescription()).orElse(po.getDescription()));
        po.setBucketName(bucket);
        po.setObjectKey(objectKey);
        po.setDownloadUrl(url);
        po.setContentType(contentType);
        po.setSize(size);

        LanguagePackPo saved = repository.save(po);
        return mapper.toVo(saved);
    }

    /**
     * 下载语言包文件（流式传输）
     */
    public ResponseEntity<Resource> download(String locale) {
        LanguagePackPo po = repository.findByLocale(locale)
                .orElseThrow(() -> new LanguagePackException("语言包不存在"));
        if (!StringUtils.hasText(po.getBucketName()) || !StringUtils.hasText(po.getObjectKey())) {
            throw new LanguagePackException("语言包未上传文件");
        }
        String fileName = locale + ".json";
        return minioUtils.streamObject(po.getBucketName(), po.getObjectKey(), fileName, po.getContentType());
    }
}

