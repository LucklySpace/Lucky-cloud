package com.xy.lucky.platform.service;

import com.xy.lucky.platform.domain.po.AssetPo;
import com.xy.lucky.platform.domain.po.ReleasePo;
import com.xy.lucky.platform.domain.vo.AssetVo;
import com.xy.lucky.platform.domain.vo.CreateAssetVo;
import com.xy.lucky.platform.domain.vo.CreateReleaseVo;
import com.xy.lucky.platform.domain.vo.ReleaseVo;
import com.xy.lucky.platform.exception.ReleaseException;
import com.xy.lucky.platform.mapper.ReleaseAssetVoMapper;
import com.xy.lucky.platform.repository.UpdateAssetRepository;
import com.xy.lucky.platform.repository.UpdateReleaseRepository;
import com.xy.lucky.platform.storage.MinioProperties;
import com.xy.lucky.platform.storage.MinioStorageService;
import com.xy.lucky.platform.utils.MD5Utils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * 版本发布服务
 * 处理版本发布和资产管理
 */
@Slf4j
@Service
public class ReleaseService {

    private final UpdateReleaseRepository releaseRepository;
    private final UpdateAssetRepository assetRepository;
    private final MinioStorageService minioStorageService;
    private final MinioProperties minioProperties;
    private final ReleaseAssetVoMapper releaseAssetVoMapper;

    @Autowired
    public ReleaseService(UpdateReleaseRepository releaseRepository,
                          UpdateAssetRepository assetRepository,
                          MinioStorageService minioStorageService,
                          MinioProperties minioProperties,
                          ReleaseAssetVoMapper releaseAssetVoMapper) {
        this.releaseRepository = releaseRepository;
        this.assetRepository = assetRepository;
        this.minioStorageService = minioStorageService;
        this.minioProperties = minioProperties;
        this.releaseAssetVoMapper = releaseAssetVoMapper;
    }

    /**
     * 发布或更新版本信息
     *
     * @param createReleaseVo 版本创建信息
     * @return 版本信息
     */
    @Transactional
    public ReleaseVo publishRelease(CreateReleaseVo createReleaseVo) {
        log.info("开始处理版本发布请求，应用ID: {}，版本号: {}", createReleaseVo.getAppId(), createReleaseVo.getVersion());

        Optional<ReleasePo> existingRelease = releaseRepository.findByAppIdAndVersion(
                createReleaseVo.getAppId(), createReleaseVo.getVersion());

        ReleasePo release;
        if (existingRelease.isPresent()) {
            // 更新现有版本
            release = existingRelease.get();
            log.debug("发现现有版本，进行更新");

            if (StringUtils.hasText(createReleaseVo.getNotes())) {
                release.setNotes(createReleaseVo.getNotes());
            }

            if (StringUtils.hasText(createReleaseVo.getPubDate())) {
                release.setPubDate(createReleaseVo.getPubDate());
            }
        } else {
            // 创建新版本
            log.debug("未发现现有版本，创建新版本");

            release = ReleasePo.builder()
                    .appId(createReleaseVo.getAppId())
                    .version(createReleaseVo.getVersion())
                    .notes(createReleaseVo.getNotes())
                    .pubDate(createReleaseVo.getPubDate())
                    .build();
        }

        ReleasePo savedRelease = releaseRepository.save(release);
        log.info("版本发布完成，版本ID: {}", savedRelease.getId());
        return releaseAssetVoMapper.toVo(savedRelease);
    }

    /**
     * 发布资产（上传文件并关联到版本）
     *
     * @param createAssetVo 资产创建信息
     * @param file          上传的文件
     * @return 资产信息
     */
    @Transactional
    public AssetVo publishAsset(CreateAssetVo createAssetVo, MultipartFile file) {
        log.info("开始处理资产发布请求，版本号: {}，平台: {}", createAssetVo.getVersion(), createAssetVo.getPlatform());

        // 验证文件
        if (file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
            log.warn("上传的文件为空");
            throw new ReleaseException("上传的文件为空");
        }

        // 验证文件MD5
        MD5Utils.checkMD5(createAssetVo.getMd5(), file);

        String filename = file.getOriginalFilename();

        // 查找关联的版本
        ReleasePo release = releaseRepository.findById(createAssetVo.getReleaseId())
                .orElseThrow(() -> {
                    log.warn("未找到指定的版本，版本ID: {}", createAssetVo.getReleaseId());
                    return new ReleaseException("指定的版本不存在");
                });

        log.debug("找到关联的版本，ID={}，版本号={}", release.getId(), release.getVersion());

        // 验证资产
        assetRepository.findByReleaseIdAndPlatform(createAssetVo.getReleaseId(), createAssetVo.getPlatform()).ifPresent(
                assetPo -> {
                    log.warn("已存在相同平台资产，请勿重复上传");
                    throw new ReleaseException("已存在相同平台资产，请勿重复上传");
                }
        );

        // 构造存储路径
        String bucket = minioProperties.getBucketName();
        String objectKey = String.format("releases/%s/%s/%s", release.getVersion(), createAssetVo.getPlatform(), filename);
        String contentType = file.getContentType();
        long size = file.getSize();

        log.info("开始上传文件到MinIO，存储桶={}，对象键={}，大小={}字节", bucket, objectKey, size);

        try {
            // 上传到MinIO
            minioStorageService.uploadObject(bucket, objectKey, file.getInputStream(), size, contentType);
            log.debug("文件上传成功");
        } catch (Exception e) {
            log.error("文件上传失败，存储桶={}，对象键={}", bucket, objectKey, e);
            throw new ReleaseException("文件上传失败: " + e.getMessage());
        }

        // 创建资产记录
        AssetPo assetPo = AssetPo.builder()
                .release(release)
                .fileName(filename)
                .platform(createAssetVo.getPlatform())
                .signature(createAssetVo.getSignature())
                .bucketName(bucket)
                .objectKey(objectKey)
                .contentType(contentType)
                .url(minioStorageService.presignedGetUrl(bucket, objectKey, 60 * 60 * 24))
                .fileSize(size)
                .build();

        AssetPo savedAsset = assetRepository.save(assetPo);
        log.info("资产发布完成，资产ID: {}", savedAsset.getId());

        return releaseAssetVoMapper.toVo(savedAsset);
    }
}