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
              AND (:service IS NULL OR service = :service)
              AND (:env IS NULL OR env = :env)
              AND (:keyword IS NULL OR lower(message) LIKE ('%' || lower(:keyword) || '%'))
            ORDER BY ts DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<LogPo> queryRange(
            @Param("module") String module,
            @Param("start") Instant start,
            @Param("end") Instant end,
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
            WHERE (:module IS NULL OR module = :module)
              AND ts BETWEEN :start AND :end
              AND (:level IS NULL OR level = :level)
              AND (:service IS NULL OR service = :service)
              AND (:env IS NULL OR env = :env)
              AND (:keyword IS NULL OR lower(message) LIKE ('%' || lower(:keyword) || '%'))
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> queryHistogram(
            @Param("module") String module,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("level") String level,
            @Param("service") String service,
            @Param("env") String env,
            @Param("keyword") String keyword,
            @Param("format") String format
    );

    Long deleteByTsBefore(Instant cutoff);

    Long deleteByModuleAndTsBefore(String module, Instant cutoff);

    @Query(value = "SELECT DISTINCT service FROM im_logs ORDER BY service", nativeQuery = true)
    List<String> listServices();

    @Query(value = "SELECT DISTINCT service FROM im_logs WHERE (:env IS NULL OR env = :env) ORDER BY service", nativeQuery = true)
    List<String> listServicesByEnv(@Param("env") String env);

    @Query(value = "SELECT DISTINCT module FROM im_logs ORDER BY module", nativeQuery = true)
    List<String> listModules();

    @Query(value = "SELECT DISTINCT address FROM im_logs WHERE (:service IS NULL OR service = :service) ORDER BY address", nativeQuery = true)
    List<String> listAddresses(@Param("service") String service);

    @Query(value = """
            SELECT service, COUNT(*) AS cnt
            FROM im_logs
            WHERE ts BETWEEN :start AND :end
            GROUP BY service
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topServices(@Param("start") Instant start, @Param("end") Instant end, @Param("limit") int limit);

    @Query(value = """
            SELECT address, COUNT(*) AS cnt
            FROM im_logs
            WHERE ts BETWEEN :start AND :end
            GROUP BY address
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topAddresses(@Param("start") Instant start, @Param("end") Instant end, @Param("limit") int limit);

    @Query(value = """
            SELECT split_part(exception, ':', 1) AS type, COUNT(*) AS cnt
            FROM im_logs
            WHERE exception IS NOT NULL AND exception <> '' AND ts BETWEEN :start AND :end
            GROUP BY type
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topErrorTypes(@Param("start") Instant start, @Param("end") Instant end, @Param("limit") int limit);
}
