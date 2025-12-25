package com.xy.lucky.platform.service;

import com.xy.lucky.platform.config.MinioProperties;
import com.xy.lucky.platform.domain.po.EmojiPackPo;
import com.xy.lucky.platform.domain.po.EmojiPo;
import com.xy.lucky.platform.domain.vo.EmojiPackVo;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
        return packRepository.findAll().stream()
                .map(emojiVoMapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 上传表情图片到指定表情包
     */
    @Transactional
    public EmojiVo uploadEmoji(EmojiVo meta, MultipartFile file) {
        String packId = Optional.ofNullable(meta.getPackId()).map(String::trim)
                .orElseThrow(() -> new EmojiException("packId 不能为空"));
        String name = Optional.ofNullable(meta.getName()).map(String::trim)
                .orElseThrow(() -> new EmojiException("name 不能为空"));
        if (file == null || file.isEmpty()) {
            throw new EmojiException("文件不能为空");
        }

        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));

        emojiRepository.findByPackIdAndName(packId, name).ifPresent(e -> {
            throw new EmojiException("已存在同名表情");
        });

        String bucket = minioProperties.getBucketName();
        String filename = Optional.ofNullable(file.getOriginalFilename())
                .filter(StringUtils::hasText).orElse(name);

        // 获取对象名称
        String objectName = minioUtils.getObjectName(filename);

        String objectKey = String.format("emoji/%s/%s", Optional.ofNullable(pack.getCode()).orElse(pack.getId()), objectName);
        String contentType = file.getContentType();
        long size = file.getSize();

        try {
            minioUtils.uploadObject(bucket, objectKey, file.getInputStream(), size, contentType);
        } catch (Exception e) {
            throw new EmojiException("文件上传失败: " + e.getMessage());
        }

        String url = minioUtils.presignedGetUrl(bucket, objectKey, 60 * 60 * 24);

        EmojiPo emo = EmojiPo.builder()
                .pack(pack)
                .name(name)
                .tags(meta.getTags())
                .bucket(bucket)
                .objectKey(objectKey)
                .url(url)
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
        return emojiRepository.findByPackId(packId).stream()
                .map(emojiVoMapper::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量上传表情（同一包）
     * - 名称默认取原始文件名
     * - 发现同名表情则跳过该文件
     */
    @Transactional
    public List<EmojiVo> uploadEmojiBatch(String packId, List<MultipartFile> files, String tags) {
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

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = Optional.ofNullable(file.getOriginalFilename()).filter(StringUtils::hasText)
                    .orElse("emoji.png");

            // 获取对象名称
            String objectName = minioUtils.getObjectName(filename);

            if (emojiRepository.findByPackIdAndName(packId, objectName).isPresent()) {
                log.warn("跳过已存在同名表情 name={}", objectName);
                continue;
            }

            String objectKey = String.format("emoji/%s/items/%s", Optional.ofNullable(pack.getCode()).orElse(pack.getId()), objectName);

            try {
                minioUtils.uploadObject(bucket, objectKey, file.getInputStream(), file.getSize(), file.getContentType());
            } catch (Exception e) {
                log.error("批量上传失败 name={} err={}", objectName, e.getMessage());
                continue;
            }

            String url = minioUtils.presignedGetUrl(bucket, objectKey);

            EmojiPo emo = EmojiPo.builder()
                    .pack(pack)
                    .name(filename)
                    .tags(tags)
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .url(url)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            result.add(emojiVoMapper.toVo(emojiRepository.save(emo)));
        }
        return result;
    }

    /**
     * 下载表情文件（流式传输）
     */
    public ResponseEntity<Resource> downloadEmoji(String emojiId) {
        EmojiPo emo = emojiRepository.findById(emojiId)
                .orElseThrow(() -> new EmojiException("表情不存在"));
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
    public EmojiPackVo uploadCover(String packId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmojiException("封面文件不能为空");
        }
        EmojiPackPo pack = packRepository.findById(packId)
                .orElseThrow(() -> new EmojiException("表情包不存在"));

        String bucket = minioProperties.getBucketName();
        String filename = Optional.ofNullable(file.getOriginalFilename()).filter(StringUtils::hasText)
                .orElse("cover.png");

        String objectName = minioUtils.getObjectName(filename);

        String objectKey = String.format("emoji/%s/cover/%s", Optional.ofNullable(pack.getCode()).orElse(pack.getId()), objectName);

        try {
            minioUtils.uploadObject(bucket, objectKey, file.getInputStream(), file.getSize(), file.getContentType());
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
     * 启用/禁用表情包（按 code）
     */
    @Transactional
    public EmojiPackVo togglePack(String code, boolean enabled) {
        EmojiPackPo pack = packRepository.findByCode(code)
                .orElseThrow(() -> new EmojiException("表情包不存在"));
        pack.setEnabled(enabled);
        EmojiPackPo saved = packRepository.save(pack);
        return emojiVoMapper.toVo(saved);
    }

    /**
     * 查询表情包详情（按 code）
     */
    public EmojiPackVo getPack(String code) {
        EmojiPackPo pack = packRepository.findByCode(code)
                .orElseThrow(() -> new EmojiException("表情包不存在"));
        return emojiVoMapper.toVo(pack);
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
    public String getPackCode() {
        return IdUtils.base62Uuid();
    }
}
