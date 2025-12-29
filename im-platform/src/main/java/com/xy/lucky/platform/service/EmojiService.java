package com.xy.lucky.platform.service;

import com.xy.lucky.platform.config.MinioProperties;
import com.xy.lucky.platform.domain.po.EmojiPackPo;
import com.xy.lucky.platform.domain.po.EmojiPo;
import com.xy.lucky.platform.domain.vo.EmojiPackVo;
import com.xy.lucky.platform.domain.vo.EmojiRespVo;
import com.xy.lucky.platform.domain.vo.EmojiVo;
import com.xy.lucky.platform.exception.EmojiException;
import com.xy.lucky.platform.mapper.EmojiVoMapper;
import com.xy.lucky.platform.repository.EmojiPackRepository;
import com.xy.lucky.platform.repository.EmojiRepository;
import com.xy.lucky.platform.utils.MinioUtils;
import com.xy.lucky.utils.id.IdUtils;
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
public class EmojiService {

    private final EmojiPackRepository packRepository;
    private final EmojiRepository emojiRepository;
    private final EmojiVoMapper emojiVoMapper;
    private final MinioUtils minioUtils;
    private final MinioProperties minioProperties;

    /**
     * 创建或更新表情包
     */
    @Transactional
    public EmojiPackVo upsertPack(EmojiPackVo request) {

        String code = Optional.ofNullable(request.getCode()).map(String::trim)
                .orElseThrow(() -> new EmojiException("code 不能为空"));

        String name = Optional.ofNullable(request.getName()).map(String::trim)
                .orElseThrow(() -> new EmojiException("name 不能为空"));

        EmojiPackPo po = packRepository.findByCode(code).orElse(EmojiPackPo.builder().code(code).build());
        po.setName(name);
        po.setDescription(request.getDescription());
        po.setEnabled(Optional.ofNullable(request.getEnabled()).orElse(Boolean.TRUE));
        EmojiPackPo saved = packRepository.save(po);
        return emojiVoMapper.toVo(saved);
    }

    /**
     * 列出所有表情包
     */
    public List<EmojiPackVo> listPacks() {
        return packRepository.findAllByOrderByHeatDesc().stream()
                .map(emojiVoMapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 上传表情图片到指定表情包
     */
    @Transactional
    public EmojiVo uploadEmoji(EmojiVo meta) {
        String packId = Optional.ofNullable(meta.getPackId()).map(String::trim)
                .orElseThrow(() -> new EmojiException("packId 不能为空"));
        String name = Optional.ofNullable(meta.getName()).map(String::trim)
                .orElseThrow(() -> new EmojiException("name 不能为空"));

        FilePart file = meta.getFile();

        if (file == null || !StringUtils.hasText(file.filename())) {
            throw new EmojiException("文件不能为空");
        }

        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));

        emojiRepository.findByPackIdAndName(packId, name).ifPresent(e -> {
            throw new EmojiException("已存在同名表情");
        });

        String bucket = minioProperties.getBucketName();
        String filename = Optional.ofNullable(file.filename())
                .filter(StringUtils::hasText).orElse(name);

        // 获取对象名称
        String objectName = minioUtils.getObjectName(filename);

        String objectKey = String.format("emoji/%s/%s", pack.getId(), objectName);
        String contentType = file.headers() != null && file.headers().getContentType() != null
                ? file.headers().getContentType().toString() : null;
        Path temp;
        long size;
        try {
            temp = Files.createTempFile("emoji-", "-" + filename);
            file.transferTo(temp.toFile()).block();
            size = Files.size(temp);
        } catch (Exception e) {
            throw new EmojiException("文件接收失败: " + e.getMessage());
        }

        try {
            try (InputStream in = Files.newInputStream(temp)) {
                minioUtils.uploadObject(bucket, objectKey, in, size, contentType);
            }
        } catch (Exception e) {
            throw new EmojiException("文件上传失败: " + e.getMessage());
        }
        try {
            Files.deleteIfExists(temp);
        } catch (Exception ignore) {
        }

        String url = minioUtils.presignedGetUrl(bucket, objectKey, 60 * 60 * 24);

        int sort = Optional.ofNullable(meta.getSort()).orElseGet(() -> emojiRepository.findMaxSortByPackId(packId) + 1);

        EmojiPo emo = EmojiPo.builder()
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

        EmojiPo saved = emojiRepository.save(emo);
        return emojiVoMapper.toVo(saved);
    }

    /**
     * 列出指定表情包中的所有表情
     */
    public List<EmojiVo> listEmojis(String packId) {
        return emojiRepository.findByPackIdOrderBySortAsc(packId).stream()
                .map(emojiVoMapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量上传表情（同一包）
     * - 名称默认取原始文件名
     * - 发现同名表情则跳过该文件
     */
    @Transactional
    public List<EmojiVo> uploadEmojiBatch(String packId, List<FilePart> files, String tags) {
        if (!StringUtils.hasText(packId)) {
            throw new EmojiException("packId 不能为空");
        }
        if (files == null || files.isEmpty()) {
            throw new EmojiException("文件列表不能为空");
        }
        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));

        String bucket = minioProperties.getBucketName();
        List<EmojiVo> result = new ArrayList<>();

        int baseSort = emojiRepository.findMaxSortByPackId(packId);
        for (FilePart file : files) {
            if (file == null || !StringUtils.hasText(file.filename())) {
                continue;
            }
            String filename = Optional.of(file.filename()).filter(StringUtils::hasText)
                    .orElse("emoji.png");

            // 获取对象名称
            String objectName = minioUtils.getObjectName(filename);

            if (emojiRepository.findByPackIdAndName(packId, objectName).isPresent()) {
                log.warn("跳过已存在同名表情 name={}", objectName);
                continue;
            }

            String objectKey = String.format("emoji/%s/items/%s", pack.getId(), objectName);

            try {
                Path temp = Files.createTempFile("emoji-batch-", "-" + filename);
                file.transferTo(temp.toFile()).block();
                long size = Files.size(temp);
                String ct = file.headers() != null && file.headers().getContentType() != null
                        ? file.headers().getContentType().toString() : null;
                try (InputStream in = Files.newInputStream(temp)) {
                    minioUtils.uploadObject(bucket, objectKey, in, size, ct);
                }
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignore) {
                }

                String url = minioUtils.presignedGetUrl(bucket, objectKey);

                baseSort += 1;
                EmojiPo emo = EmojiPo.builder()
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

                result.add(emojiVoMapper.toVo(emojiRepository.save(emo)));
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
    public ResponseEntity<Resource> downloadEmoji(String emojiId) {
        EmojiPo emo = emojiRepository.findById(emojiId)
                .orElseThrow(() -> new EmojiException("表情不存在"));
        try {
            packRepository.incrementHeatById(emo.getPack().getId(), 1L);
        } catch (Exception e) {
            log.warn("热度更新失败 packId={} err={}", emo.getPack().getId(), e.getMessage());
        }
        return minioUtils.streamObject(
                emo.getBucket(),
                emo.getObjectKey(),
                emo.getName(),
                emo.getContentType()
        );
    }

    /**
     * 上传表情包封面图
     */
    @Transactional
    public EmojiPackVo uploadCover(String packId, FilePart file) {
        if (file == null || !StringUtils.hasText(file.filename())) {
            throw new EmojiException("封面文件不能为空");
        }
        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));

        String bucket = minioProperties.getBucketName();
        String filename = Optional.of(file.filename()).filter(StringUtils::hasText)
                .orElse("cover.png");

        String objectName = minioUtils.getObjectName(filename);

        String objectKey = String.format("emoji/%s/cover/%s", pack.getId(), objectName);

        try {
            java.nio.file.Path temp = java.nio.file.Files.createTempFile("emoji-cover-", "-" + filename);
            file.transferTo(temp.toFile()).block();
            long size = java.nio.file.Files.size(temp);
            String ct = file.headers() != null && file.headers().getContentType() != null
                    ? file.headers().getContentType().toString() : null;
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(temp)) {
                minioUtils.uploadObject(bucket, objectKey, in, size, ct);
            }
            try {
                java.nio.file.Files.deleteIfExists(temp);
            } catch (Exception ignore) {
            }
        } catch (Exception e) {
            throw new EmojiException("封面上传失败: " + e.getMessage());
        }

        String url = minioUtils.presignedGetUrl(bucket, objectKey);
        pack.setBucket(bucket);
        pack.setObjectKey(objectKey);
        pack.setUrl(url);

        EmojiPackPo saved = packRepository.save(pack);
        return emojiVoMapper.toVo(saved);
    }

    /**
     * 启用/禁用表情包（按 packId）
     */
    @Transactional
    public EmojiPackVo togglePack(String packId, boolean enabled) {
        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));
        pack.setEnabled(enabled);
        EmojiPackPo saved = packRepository.save(pack);
        return emojiVoMapper.toVo(saved);
    }

    /**
     * 查询表情包详情（按 packId）
     */
    public EmojiRespVo getPackId(String packId) {

        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));

        EmojiRespVo vo = emojiVoMapper.toRespVo(pack);

        List<EmojiVo> list = emojiRepository.findByPackIdOrderBySort(packId)
                .stream().map(emojiVoMapper::toVo).toList();

        if (!list.isEmpty()) {
            vo.setEmojis(list);
        }

        return vo;
    }

    /**
     * 删除表情（可选同时删除对象存储）
     */
    @Transactional
    public void deleteEmoji(String emojiId, boolean removeObject) {
        EmojiPo emo = emojiRepository.findById(emojiId)
                .orElseThrow(() -> new EmojiException("表情不存在"));
        if (removeObject) {
            minioUtils.removeObject(emo.getBucket(), emo.getObjectKey());
        }
        emojiRepository.deleteById(emojiId);
    }

    /**
     * 生成表情包编码
     */
    public String getPackCode(int i) {
        if (i % 5 > 0) {
            throw new EmojiException("生成表情包编码异常");
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
}
