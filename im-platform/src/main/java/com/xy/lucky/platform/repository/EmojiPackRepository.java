package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.EmojiPackPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmojiPackRepository extends JpaRepository<EmojiPackPo, String> {

    Optional<EmojiPackPo> findByCode(String code);

    Optional<EmojiPackPo> findById(String id);

    boolean existsByCode(String code);

    List<EmojiPackPo> findAllByOrderByHeatDesc();

    @Modifying
    @Query("update EmojiPackPo p set p.heat = p.heat + :delta where p.id = :id")
    int incrementHeatById(@Param("id") String id, @Param("delta") long delta);
}
