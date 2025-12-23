package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.AssetPo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UpdateAssetRepository extends JpaRepository<AssetPo, String> {
    Optional<AssetPo> findByFileName(String fileName);

    List<AssetPo> findByReleaseId(String releaseId);

    Optional<AssetPo> findByReleaseIdAndPlatform(String releaseId, String platform);
}
