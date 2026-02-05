package com.xy.lucky.leaf.repository;

import com.xy.lucky.leaf.model.IdMetaInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ID元信息数据访问接口
 * 提供对IdMetaInfo实体的基本CRUD操作
 */
public interface IdMetaInfoRepository extends JpaRepository<IdMetaInfo, String> {
}