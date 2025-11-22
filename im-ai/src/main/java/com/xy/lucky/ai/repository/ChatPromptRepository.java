package com.xy.lucky.ai.repository;

import com.xy.lucky.ai.domain.ChatPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatPromptRepository extends JpaRepository<ChatPrompt, String> {

}
