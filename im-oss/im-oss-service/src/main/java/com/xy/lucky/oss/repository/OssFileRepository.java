package com.xy.lucky.oss.repository;

import com.xy.lucky.oss.domain.po.OssFilePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OssFileRepository extends JpaRepository<OssFilePo, String> {
    Optional<OssFilePo> findByIdentifier(String identifier);
}
