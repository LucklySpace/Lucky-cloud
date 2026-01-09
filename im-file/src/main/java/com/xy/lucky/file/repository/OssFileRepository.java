package com.xy.lucky.file.repository;

import com.xy.lucky.file.domain.po.OssFilePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OssFileRepository extends JpaRepository<OssFilePo, String> {
    Optional<OssFilePo> findByIdentifier(String identifier);
}
