package com.xy.lucky.ai.repository;

import com.xy.lucky.ai.domain.po.ChatSessionPo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionPo, String> {

    List<ChatSessionPo> findByUserId(String userId);

}
