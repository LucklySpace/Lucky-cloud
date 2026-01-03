package com.xy.lucky.logging.repository;

import com.xy.lucky.logging.domain.po.LogPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface LogRepository extends JpaRepository<LogPo, String>, JpaSpecificationExecutor<LogPo>, QueryByExampleExecutor<LogPo> {

    @Query(value = """
            SELECT *
            FROM im_logs
            WHERE (:module IS NULL OR lower(module) = lower(:module))
              AND ts BETWEEN :start AND :end
              AND (:level IS NULL OR lower(level) = lower(:level))
              AND (:service IS NULL OR lower(service) = lower(:service))
              AND (:env IS NULL OR lower(env) = lower(:env))
              AND (:keyword IS NULL OR lower(message) LIKE ('%' || lower(:keyword) || '%'))
            ORDER BY ts DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<LogPo> queryRange(
            @Param("module") String module,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("level") String level,
            @Param("service") String service,
            @Param("env") String env,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    @Query(value = """
            SELECT to_char(ts, :format) as bucket, COUNT(*) as cnt
            FROM im_logs
            WHERE (:module IS NULL OR lower(module) = lower(:module))
              AND ts BETWEEN :start AND :end
              AND (:level IS NULL OR lower(level) = lower(:level))
              AND (:service IS NULL OR lower(service) = lower(:service))
              AND (:env IS NULL OR lower(env) = lower(:env))
              AND (:keyword IS NULL OR lower(message) LIKE ('%' || lower(:keyword) || '%'))
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> queryHistogram(
            @Param("module") String module,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("level") String level,
            @Param("service") String service,
            @Param("env") String env,
            @Param("keyword") String keyword,
            @Param("format") String format
    );

    Boolean deleteByTsBefore(LocalDateTime cutoff);

    Boolean deleteByModuleAndTsBefore(String module, LocalDateTime cutoff);

    @Query(value = """
            SELECT service, COUNT(*) AS cnt
            FROM im_logs
            WHERE ts BETWEEN :start AND :end
            GROUP BY service
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topServices(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);

    @Query(value = """
            SELECT address, COUNT(*) AS cnt
            FROM im_logs
            WHERE ts BETWEEN :start AND :end
            GROUP BY address
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topAddresses(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);

    @Query(value = """
            SELECT split_part(exception, ':', 1) AS type, COUNT(*) AS cnt
            FROM im_logs
            WHERE exception IS NOT NULL AND exception <> '' AND ts BETWEEN :start AND :end
            GROUP BY type
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topErrorTypes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);
}
