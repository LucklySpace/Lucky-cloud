package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.StickerPackPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StickerPackRepository extends JpaRepository<StickerPackPo, String> {

    Optional<StickerPackPo> findByCode(String code);

    Optional<StickerPackPo> findById(String id);

    boolean existsByCode(String code);

    List<StickerPackPo> findAllByOrderByHeatDesc();

    @Modifying
    @Query("update StickerPackPo p set p.heat = p.heat + :delta where p.id = :id")
    int incrementHeatById(@Param("id") String id, @Param("delta") long delta);
}
