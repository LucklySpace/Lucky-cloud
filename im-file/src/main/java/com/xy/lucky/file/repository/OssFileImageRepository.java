package com.xy.lucky.file.repository;

import com.xy.lucky.file.domain.po.OssFileImagePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OssFileImageRepository extends JpaRepository<OssFileImagePo, Long> {
    Optional<OssFileImagePo> findByIdentifier(String identifier);
}
