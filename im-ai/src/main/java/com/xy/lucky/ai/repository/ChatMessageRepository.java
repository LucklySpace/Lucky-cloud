package com.xy.lucky.ai.repository;

import com.xy.lucky.ai.domain.po.ChatMessagePo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessagePo, String> {

    List<ChatMessagePo> findBySession_IdOrderByCreatedAtDesc(String sessionId);

    List<ChatMessagePo> findBySession_Id(String sessionId, Pageable pageable);

    void deleteBySession_Id(String sessionId);
}
