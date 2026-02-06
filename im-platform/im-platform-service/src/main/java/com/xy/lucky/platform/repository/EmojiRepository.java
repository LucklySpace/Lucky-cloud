package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.EmojiPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmojiRepository extends JpaRepository<EmojiPo, String> {

    List<EmojiPo> findByPackIdOrderBySort(String packId);

    Optional<EmojiPo> findByPackIdAndName(String packId, String name);

    List<EmojiPo> findByPackIdOrderBySortAsc(String packId);

    @Query("select coalesce(max(e.sort), 0) from EmojiPo e where e.pack.id = :packId")
    int findMaxSortByPackId(@Param("packId") String packId);
}
