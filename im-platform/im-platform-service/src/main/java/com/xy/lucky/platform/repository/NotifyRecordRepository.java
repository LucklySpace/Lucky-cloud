package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.NotifyRecordPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifyRecordRepository extends JpaRepository<NotifyRecordPo, String> {
}
