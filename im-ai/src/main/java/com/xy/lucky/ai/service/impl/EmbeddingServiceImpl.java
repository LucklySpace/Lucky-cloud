package com.xy.lucky.ai.service.impl;

import com.xy.lucky.ai.service.EmbeddingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Resource
    private PgVectorStore pgVectorStore;

    @Override
    public String embed(String text) {
        return "";
    }

    /**
     * 存入向量数据库，这个过程会自动调用embeddingModel,将文本变成向量再存入。
     * 解析指定的 PDF 文件，并返回封装后的 Document 对象
     *
     * @param file PDF 文件对象
     * @return Document 对象，包含文本内容和元数据
     * @throws Exception 解析过程中抛出的异常
     */
    @Override
    public void add(MultipartFile file) {

        // 将文本内容划分成更小的块
        List<Document> documents = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        try {
            InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream());

            if (file.getName().endsWith(".pdf")) {
                DocumentReader pdfDocumentReader = new PagePdfDocumentReader(inputStreamResource);
                // 将文本内容划分成更小的块
                documents = new TokenTextSplitter()
                        .apply(pdfDocumentReader.read());
            } else {
                // 创建TikaDocumentReader对象，并读取文档
                DocumentReader tikaDocumentReader = new TikaDocumentReader(inputStreamResource);
                // 将文本内容划分成更小的块
                documents = new TokenTextSplitter()
                        .apply(tikaDocumentReader.read());
            }
            log.debug("文件解析成功: {} , 解析时长: {}ms", file.getName(), (System.currentTimeMillis() - startTime));

        } catch (Exception e) {

            log.error("{} 解析失败", file.getName());
        }

        if (!documents.isEmpty()) {
            pgVectorStore.add(documents);
        }
    }

    @Override
    public List<String> search(String text) {

        List<Document> documents = pgVectorStore.similaritySearch(SearchRequest.builder().query(text).topK(1).build());
        return documents.stream().map(Document::getText).collect(Collectors.toList());
    }
}
