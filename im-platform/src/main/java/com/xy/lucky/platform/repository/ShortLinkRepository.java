package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.ShortLinkPo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 短链数据仓库
 */
public interface ShortLinkRepository extends JpaRepository<ShortLinkPo, UUID> {
    /**
     * 根据短码查询
     */
    Optional<ShortLinkPo> findByShortCode(String shortCode);

    /**
     * 根据原始URL查询
     */
    Optional<ShortLinkPo> findTopByOriginalUrlOrderByCreateTimeDesc(String originalUrl);

    /**
     * 查询未过期且启用的短链
     */
    List<ShortLinkPo> findByEnabledTrueAndExpireTimeAfter(LocalDateTime now);
}

