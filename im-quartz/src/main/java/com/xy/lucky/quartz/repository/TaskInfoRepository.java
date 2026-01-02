package com.xy.lucky.quartz.repository;

import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskInfoRepository extends JpaRepository<TaskInfoPo, Long> {
    Optional<TaskInfoPo> findByJobName(String jobName);

    boolean existsByJobName(String jobName);

    long countByStatus(TaskStatus status);
}
