package com.xy.lucky.quartz.repository;

import com.xy.lucky.quartz.domain.po.JobRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRegistryRepository extends JpaRepository<JobRegistry, Long> {

    List<JobRegistry> findByAppNameAndStatus(String appName, Integer status);

    JobRegistry findByAppNameAndAddress(String appName, String address);

    @Modifying
    @Query("UPDATE JobRegistry r SET r.status = 0 WHERE r.updateTime < :threshold")
    int updateDead(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM JobRegistry r WHERE r.updateTime < :threshold")
    int deleteDead(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT DISTINCT r.appName FROM JobRegistry r")
    List<String> findDistinctAppNames();
}
