package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.EmojiPackPo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmojiPackRepository extends JpaRepository<EmojiPackPo, String> {
    Optional<EmojiPackPo> findByCode(String code);

    boolean existsByCode(String code);
}

