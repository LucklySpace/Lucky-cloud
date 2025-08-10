package com.xy.ai.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmbeddingService {

    String embed(String text);

    void add(MultipartFile file);

    List<String> search(String text);
}
