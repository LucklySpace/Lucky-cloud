package com.xy.lucky.platform.repository;

import com.xy.lucky.platform.domain.po.LanguagePackPo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguagePackRepository extends JpaRepository<LanguagePackPo, String> {

    Optional<LanguagePackPo> findByLocale(String locale);
}

