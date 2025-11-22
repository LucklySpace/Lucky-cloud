//package com.xy.ai.controller;
//
//import org.springframework.ai.document.Document;
//import org.springframework.ai.reader.tika.TikaDocumentReader;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//import static java.util.stream.Collectors.toList;
//
/// **
// * @Author majinzhong
// * @Date 2025/4/30 14:00
// * @Version 1.0
// */
//@RestController
//public class EmbeddingController {
//
//    @Autowired
//    VectorStore vectorStore;
//
//    @PostMapping("/ai/vectorStore")
//    public List<String> vectorStore(@RequestParam(name = "file") MultipartFile file) throws Exception {
//        // 从IO流中读取文件
//        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
//        // 将文本内容划分成更小的块
//        List<Document> splitDocuments = new TokenTextSplitter()
//                .apply(tikaDocumentReader.read());
//        // 存入向量数据库，这个过程会自动调用embeddingModel,将文本变成向量再存入。
//        vectorStore.add(splitDocuments);
//
//        return splitDocuments.stream().map(Document::getText).collect(toList());
//    }
//
//    @GetMapping("/ai/vectorSearch")
//    public List<String> vectorSearch(@RequestParam(name = "text") String text) {
//
//        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(text).topK(1).build());
//
//        return documents.stream().map(Document::getText).collect(toList());
//    }
//}