package com.xy.lucky.platform.service;

import com.xy.lucky.platform.domain.po.AssetPo;
import com.xy.lucky.platform.domain.po.ReleasePo;
import com.xy.lucky.platform.domain.vo.PlatformInfoVo;
import com.xy.lucky.platform.domain.vo.UpdaterResponseVo;
import com.xy.lucky.platform.repository.UpdateAssetRepository;
import com.xy.lucky.platform.repository.UpdateReleaseRepository;
import com.xy.lucky.platform.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 应用更新服务
 * 提供版本检查与安装包下载接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdaterService {

    private final UpdateReleaseRepository releaseRepository;
    private final UpdateAssetRepository assetRepository;
    private final MinioStorageService minioStorageService;


    /**
     * 获取最新版本信息
     * 从数据库读取最新发布与其平台文件
     *
     * @return 最新版本信息响应
     */
    public UpdaterResponseVo latest() {
        log.info("正在获取最新版本信息");

        Optional<ReleasePo> latestOpt = releaseRepository.findTopByOrderByCreateTimeDesc();

        if (latestOpt.isEmpty()) {
            log.info("未找到任何发布版本");
            return new UpdaterResponseVo(null, null, null, Map.of());
        }

        ReleasePo release = latestOpt.get();
        log.debug("找到最新版本，ID={}，版本号={}", release.getId(), release.getVersion());

        List<AssetPo> assets = assetRepository.findByReleaseId(release.getId());
        log.debug("找到 {} 个平台资产", assets.size());

        Map<String, PlatformInfoVo> platforms = new HashMap<>();
        for (AssetPo asset : assets) {
            String url = generateDownloadUrl(asset);
            platforms.put(asset.getPlatform(), new PlatformInfoVo(asset.getSignature(), url));
        }

        UpdaterResponseVo response = new UpdaterResponseVo(
                release.getVersion(),
                release.getNotes(),
                release.getPubDate(),
                platforms
        );

        log.info("成功构建版本信息响应，版本号: {}", release.getVersion());
        return response;
    }

    /**
     * 生成资产下载URL
     *
     * @param asset 资产信息
     * @return 下载URL
     */
    private String generateDownloadUrl(AssetPo asset) {
        String url = null;

        // 优先使用MinIO存储
        if (StringUtils.hasText(asset.getBucketName()) && StringUtils.hasText(asset.getObjectKey())) {
            url = minioStorageService.presignedGetUrl(
                    asset.getBucketName(),
                    asset.getObjectKey(),
                    24 * 60 * 60); // 24小时有效期
            log.debug("生成MinIO预签名URL，平台={}，对象键={}", asset.getPlatform(), asset.getObjectKey());
        }
        // 回退到显式URL
        else if (StringUtils.hasText(asset.getUrl())) {
            url = asset.getUrl();
            log.debug("使用显式URL，平台={}，URL={}", asset.getPlatform(), url);
        }

        return url;
    }

    /**
     * 从存储中下载文件
     *
     * @param fileName 文件名
     * @return 文件资源响应实体
     */
    public ResponseEntity<Resource> downloadFile(String fileName) {
        log.info("开始处理文件下载请求，文件名: {}", fileName);

        // 参数校验
        if (!StringUtils.hasText(fileName)) {
            log.warn("文件名为空");
            return ResponseEntity.badRequest().build();
        }

        // 查找资产
        Optional<AssetPo> assetOpt = assetRepository.findByFileName(fileName);
        if (assetOpt.isEmpty()) {
            log.info("未找到文件对应的资产，文件名: {}", fileName);
            return ResponseEntity.notFound().build();
        }

        AssetPo asset = assetOpt.get();

        // 校验存储信息
        if (!StringUtils.hasText(asset.getBucketName()) || !StringUtils.hasText(asset.getObjectKey())) {
            log.warn("资产缺少存储信息，文件名: {}", fileName);
            return ResponseEntity.notFound().build();
        }

        log.info("开始流式传输文件，存储桶={}，对象键={}", asset.getBucketName(), asset.getObjectKey());
        return minioStorageService.streamObject(
                asset.getBucketName(),
                asset.getObjectKey(),
                asset.getFileName(),
                asset.getContentType()
        );
    }
}