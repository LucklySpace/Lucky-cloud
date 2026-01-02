package com.xy.lucky.quartz.repository;

import com.xy.lucky.quartz.domain.po.TaskLogPo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLogPo, Long> {

    Page<TaskLogPo> findByJobName(String jobName, Pageable pageable);

    long countByStartTimeAfter(LocalDateTime startTime);

    long countByStartTimeAfterAndStatus(LocalDateTime startTime, Integer status);

}
