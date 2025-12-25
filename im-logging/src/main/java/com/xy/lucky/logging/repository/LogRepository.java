package com.xy.lucky.logging.repository;

import com.xy.lucky.logging.domain.po.LogPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LogRepository extends JpaRepository<LogPo, String> {

    @Query(value = """
            SELECT *
            FROM im_logs
            WHERE (:module IS NULL OR module = :module)
              AND ts BETWEEN :start AND :end
              AND (:level IS NULL OR level = :level)
              AND (:keyword IS NULL OR lower(message) LIKE CONCAT('%', lower(:keyword), '%'))
            ORDER BY ts DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<LogPo> queryRange(
            @Param("module") String module,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("level") String level,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long deleteByTsBefore(Instant cutoff);

    Long deleteByModuleAndTsBefore(String module, Instant cutoff);
}
