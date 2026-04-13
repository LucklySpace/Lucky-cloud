package com.xy.lucky.oss.repository;

import com.xy.lucky.oss.domain.po.OssFilePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OssFileRepository extends JpaRepository<OssFilePo, String> {
    Optional<OssFilePo> findByIdentifier(String identifier);

    List<OssFilePo> findTop200ByIsFinishAndCreateTimeBeforeOrderByCreateTimeAsc(Integer isFinish, LocalDateTime createTime);

    long deleteByIdentifier(String identifier);
}
