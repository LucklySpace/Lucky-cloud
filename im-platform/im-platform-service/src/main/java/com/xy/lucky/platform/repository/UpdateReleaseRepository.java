package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.ReleasePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UpdateReleaseRepository extends JpaRepository<ReleasePo, String> {
    Optional<ReleasePo> findTopByOrderByCreateTimeDesc();

    Optional<ReleasePo> findByVersion(String version);

    Optional<ReleasePo> findByAppIdAndVersion(String appId, String version);
}

