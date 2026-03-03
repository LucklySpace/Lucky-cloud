package com.xy.lucky.platform.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.xy.lucky.oss.client.OssProperties;
import com.xy.lucky.oss.client.OssTemplate;
import com.xy.lucky.platform.domain.po.StickerPackPo;
import com.xy.lucky.platform.domain.po.StickerPo;
import com.xy.lucky.platform.domain.vo.StickerPackVo;
import com.xy.lucky.platform.domain.vo.StickerRespVo;
import com.xy.lucky.platform.domain.vo.StickerVo;
import com.xy.lucky.platform.exception.StickerException;
import com.xy.lucky.platform.mapper.StickerVoMapper;
import com.xy.lucky.platform.repository.StickerPackRepository;
import com.xy.lucky.platform.repository.StickerRepository;
import com.xy.lucky.utils.id.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 表情包管理服务
 * - 创建/查询表情包
 * - 上传/查询表情条目
 * - 文件存储于 MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StickerService {

    private final StickerPackRepository packRepository;
    private final StickerRepository stickerRepository;
    private final StickerVoMapper stickerVoMapper;
    private final OssTemplate ossTemplate;
    private final OssProperties ossProperties;

    /**
     * 创建或更新表情包
     */
    @Transactional
    public StickerPackVo upsertPack(StickerPackVo request) {

        String code = Optional.ofNullable(request.getCode()).map(String::trim)
                .orElseThrow(() -> new StickerException("code 不能为空"));

        String name = Optional.ofNullable(request.getName()).map(String::trim)
                .orElseThrow(() -> new StickerException("name 不能为空"));

        StickerPackPo po = packRepository.findByCode(code).orElse(StickerPackPo.builder().code(code).build());
        po.setName(name);
        po.setDescription(request.getDescription());
        po.setEnabled(Optional.ofNullable(request.getEnabled()).orElse(Boolean.TRUE));
        StickerPackPo saved = packRepository.save(po);
        return stickerVoMapper.toVo(saved);
    }

    /**
     * 列出所有表情包
     */
    public List<StickerPackVo> listPacks() {
        return packRepository.findAllByOrderByHeatDesc().stream()
                .map(stickerVoMapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 上传表情图片到指定表情包
     */
    @Transactional
    public StickerVo uploadSticker(StickerVo meta) {
        String packId = Optional.ofNullable(meta.getPackId()).map(String::trim)
                .orElseThrow(() -> new StickerException("packId 不能为空"));
        String name = Optional.ofNullable(meta.getName()).map(String::trim)
                .orElseThrow(() -> new StickerException("name 不能为空"));

        FilePart file = meta.getFile();

        if (file == null || !StringUtils.hasText(file.filename())) {
            throw new StickerException("文件不能为空");
        }

        StickerPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new StickerException("表情包不存在"));

        stickerRepository.findByPackIdAndName(packId, name).ifPresent(e -> {
            throw new StickerException("已存在同名表情");
        });

        String bucket = Optional.ofNullable(ossProperties.getBucketName())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new StickerException("bucket 不能为空"));
        String filename = Optional.of(file.filename())
                .filter(StringUtils::hasText).orElse(name);

        // 获取对象名称
        String objectName = getObjectName(filename);

        String objectKey = String.format("sticker/%s/%s", pack.getId(), objectName);
        String contentType = file.headers().getContentType() != null
                ? String.valueOf(file.headers().getContentType()) : null;
        Path temp;
        long size;
        try {
            temp = Files.createTempFile("sticker-", "-" + filename);
            file.transferTo(temp.toFile()).block();
            size = Files.size(temp);
        } catch (Exception e) {
            throw new StickerException("文件接收失败: " + e.getMessage());
        }

        // 上传文件 并删除临时文件
        try {
            try (InputStream in = Files.newInputStream(temp)) {
                ossTemplate.putObject(bucket, objectKey, in, contentType);
            }
        } catch (Exception e) {
            throw new StickerException("文件上传失败: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignore) {
                log.warn("删除临时文件失败: {}", temp);
            }
        }


        String url = ossTemplate.getPresignedUrl(bucket, objectKey, 24 * 60 * 60);

        int sort = Optional.ofNullable(meta.getSort()).orElseGet(() -> stickerRepository.findMaxSortByPackId(packId) + 1);

        StickerPo emo = StickerPo.builder()
                .pack(pack)
                .name(name)
                .tags(meta.getTags())
                .bucket(bucket)
                .objectKey(objectKey)
                .url(url)
                .sort(sort)
                .contentType(contentType)
                .fileSize(size)
                .build();

        StickerPo saved = stickerRepository.save(emo);
        return stickerVoMapper.toVo(saved);
    }

    /**
     * 列出指定表情包中的所有表情
     */
    public List<StickerVo> listStickers(String packId) {
        return stickerRepository.findByPackIdOrderBySortAsc(packId).stream()
                .map(stickerVoMapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量上传表情（同一包）
     * - 名称默认取原始文件名
     * - 发现同名表情则跳过该文件
     */
    @Transactional
    public List<StickerVo> uploadStickerBatch(String packId, List<FilePart> files, String tags) {
        if (!StringUtils.hasText(packId)) {
            throw new StickerException("packId 不能为空");
        }
        if (files == null || files.isEmpty()) {
            throw new StickerException("文件列表不能为空");
        }
        StickerPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new StickerException("表情包不存在"));

        String bucket = Optional.ofNullable(ossProperties.getBucketName())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new StickerException("bucket 不能为空"));
        List<StickerVo> result = new ArrayList<>();

        int baseSort = stickerRepository.findMaxSortByPackId(packId);
        for (FilePart file : files) {
            if (file == null || !StringUtils.hasText(file.filename())) {
                continue;
            }
            String filename = Optional.of(file.filename()).filter(StringUtils::hasText)
                    .orElse("sticker.png");

            // 获取对象名称
            String objectName = getObjectName(filename);

            if (stickerRepository.findByPackIdAndName(packId, filename).isPresent()) {
                log.warn("跳过已存在同名表情 name={}", filename);
                continue;
            }

            String objectKey = String.format("sticker/%s/items/%s", pack.getId(), objectName);

            try {
                Path temp = Files.createTempFile("sticker-batch-", "-" + filename);
                file.transferTo(temp.toFile()).block();
                long size = Files.size(temp);
                String ct = file.headers().getContentType() != null
                        ? String.valueOf(file.headers().getContentType()) : null;
                try (InputStream in = Files.newInputStream(temp)) {
                    ossTemplate.putObject(bucket, objectKey, in, ct);
                }
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignore) {
                    log.warn("删除临时文件失败: {}", temp);
                }

                String url = ossTemplate.getPresignedUrl(bucket, objectKey, 24 * 60 * 60);

                baseSort += 1;

                StickerPo emo = StickerPo.builder()
                        .pack(pack)
                        .name(filename)
                        .tags(tags)
                        .bucket(bucket)
                        .objectKey(objectKey)
                        .url(url)
                        .sort(baseSort)
                        .contentType(ct)
                        .fileSize(size)
                        .build();

                result.add(stickerVoMapper.toVo(stickerRepository.save(emo)));
            } catch (Exception e) {
                log.error("上传失败 name={} err={}", objectName, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 下载表情文件（流式传输）
     */
    @Transactional
    public ResponseEntity<Resource> downloadSticker(String emojiId) {
        StickerPo emo = stickerRepository.findById(emojiId)
                .orElseThrow(() -> new StickerException("表情不存在"));
        try {
            packRepository.incrementHeatById(emo.getPack().getId(), 1L);
        } catch (Exception e) {
            log.warn("热度更新失败 packId={} err={}", emo.getPack().getId(), e.getMessage());
        }
        try {
            ObjectMetadata metadata = ossTemplate.getObjectMetadata(emo.getBucket(), emo.getObjectKey());
            S3Object s3Object = ossTemplate.getObject(emo.getBucket(), emo.getObjectKey());
            InputStreamResource resource = new InputStreamResource(s3Object.getObjectContent());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(emo.getName()).build());
            headers.add(HttpHeaders.CONTENT_TYPE, metadata.getContentType());
            headers.setContentLength(metadata.getContentLength());
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 上传表情包封面图
     */
    @Transactional
    public StickerPackVo uploadCover(String packId, FilePart file) {
        if (file == null || !StringUtils.hasText(file.filename())) {
            throw new StickerException("封面文件不能为空");
        }
        StickerPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new StickerException("表情包不存在"));

        String bucket = Optional.ofNullable(ossProperties.getBucketName())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new StickerException("bucket 不能为空"));
        String filename = Optional.of(file.filename()).filter(StringUtils::hasText)
                .orElse("cover.png");

        String objectName = getObjectName(filename);

        String objectKey = String.format("emoji/%s/cover/%s", pack.getId(), objectName);

        try {
            Path temp = Files.createTempFile("emoji-cover-", "-" + filename);
            file.transferTo(temp.toFile()).block();
            String ct = file.headers() != null && file.headers().getContentType() != null
                    ? file.headers().getContentType().toString() : null;
            try (InputStream in = Files.newInputStream(temp)) {
                ossTemplate.putObject(bucket, objectKey, in, ct);
            }
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignore) {
            }
        } catch (Exception e) {
            throw new StickerException("封面上传失败: " + e.getMessage());
        }

        String url = ossTemplate.getPresignedUrl(bucket, objectKey, 24 * 60 * 60);
        pack.setBucket(bucket);
        pack.setObjectKey(objectKey);
        pack.setUrl(url);

        StickerPackPo saved = packRepository.save(pack);
        return stickerVoMapper.toVo(saved);
    }

    /**
     * 启用/禁用表情包（按 packId）
     */
    @Transactional
    public StickerPackVo togglePack(String packId, boolean enabled) {
        StickerPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new StickerException("表情包不存在"));
        pack.setEnabled(enabled);
        StickerPackPo saved = packRepository.save(pack);
        return stickerVoMapper.toVo(saved);
    }

    /**
     * 查询表情包详情（按 packId）
     */
    public StickerRespVo getPackId(String packId) {

        StickerPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new StickerException("表情包不存在"));

        StickerRespVo vo = stickerVoMapper.toRespVo(pack);

        List<StickerVo> list = stickerRepository.findByPackIdOrderBySort(packId)
                .stream().map(stickerVoMapper::toVo).toList();

        if (!list.isEmpty()) {
            vo.setStickers(list);
        }

        return vo;
    }

    /**
     * 删除表情（可选同时删除对象存储）
     */
    @Transactional
    public void deleteSticker(String emojiId, boolean removeObject) {
        StickerPo emo = stickerRepository.findById(emojiId)
                .orElseThrow(() -> new StickerException("表情不存在"));
        if (removeObject) {
            try {
                ossTemplate.deleteObject(emo.getBucket(), emo.getObjectKey());
            } catch (Exception ignore) {
            }
        }
        stickerRepository.deleteById(emojiId);
    }

    /**
     * 生成表情包编码
     */
    public String getPackCode(int i) {
        if (i % 5 > 0) {
            throw new StickerException("生成表情包编码异常");
        }
        String s = IdUtils.base62Uuid();
        log.info("生成表情包编码: {}", s);
        if (packRepository.existsByCode(s)) {
            log.info("已存在: {}", s);
            i += 1;
            return getPackCode(i);
        }
        return s;
    }

    private String getObjectName(String fileName) {
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        return IdUtils.base62Uuid() + prefix;
    }
}
