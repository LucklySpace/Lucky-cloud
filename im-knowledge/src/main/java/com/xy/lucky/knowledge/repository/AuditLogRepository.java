package com.xy.lucky.knowledge.repository;

import com.xy.lucky.knowledge.domain.po.AuditLogPo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AuditLogRepository extends ReactiveCrudRepository<AuditLogPo, Long> {
}
