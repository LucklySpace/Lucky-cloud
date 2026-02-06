package com.xy.lucky.oss.repository;

import com.xy.lucky.oss.domain.po.OssFileImagePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OssFileImageRepository extends JpaRepository<OssFileImagePo, String> {
    Optional<OssFileImagePo> findByIdentifier(String identifier);
}
