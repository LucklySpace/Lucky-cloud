package com.xy.ai.repository;

import com.xy.ai.domain.ChatPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatPromptRepository extends JpaRepository<ChatPrompt, String> {

}
