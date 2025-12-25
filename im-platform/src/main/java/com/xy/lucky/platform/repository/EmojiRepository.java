package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.EmojiPo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmojiRepository extends JpaRepository<EmojiPo, String> {
    List<EmojiPo> findByPackId(String packId);

    Optional<EmojiPo> findByPackIdAndName(String packId, String name);
}

