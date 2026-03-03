package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.StickerPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StickerRepository extends JpaRepository<StickerPo, String> {

    List<StickerPo> findByPackIdOrderBySort(String packId);

    Optional<StickerPo> findByPackIdAndName(String packId, String name);

    List<StickerPo> findByPackIdOrderBySortAsc(String packId);

    @Query("select coalesce(max(e.sort), 0) from StickerPo e where e.pack.id = :packId")
    int findMaxSortByPackId(@Param("packId") String packId);
}
