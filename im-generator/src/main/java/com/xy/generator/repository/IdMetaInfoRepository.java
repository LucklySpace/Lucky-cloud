package com.xy.generator.repository;

import com.xy.generator.model.IdMetaInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdMetaInfoRepository extends JpaRepository<IdMetaInfo, String> {
}