package com.xy.lucky.knowledge.service.impl;

import com.xy.lucky.knowledge.domain.es.EsKnowledgeDoc;
import com.xy.lucky.knowledge.domain.vo.AiChatResponse;
import com.xy.lucky.knowledge.repository.DocumentRepository;
import com.xy.lucky.knowledge.repository.GroupRepository;
import com.xy.lucky.knowledge.service.AiSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchServiceImpl implements AiSearchService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final GroupRepository groupRepository;
    private final ChatClient.Builder chatClientBuilder; // Spring AI Chat Client
    private final com.xy.lucky.knowledge.service.EsSearchService esSearchService;

    @Override
    public Mono<Void> indexDocument(Long docId, Integer version, byte[] content, String filename) {
        return Mono.fromRunnable(() -> {
            try {
                // 1. 文档解析（PDF/Tika）
                InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(content));
                DocumentReader reader;

                if (filename.toLowerCase().endsWith(".pdf")) {
                    reader = new PagePdfDocumentReader(resource);
                } else {
                    reader = new TikaDocumentReader(resource);
                }

                List<Document> documents = reader.get();
                // 2. 附加元数据
                com.xy.lucky.knowledge.domain.po.DocumentPo docEntity = documentRepository.findById(docId).block();
                Long groupId = docEntity != null ? docEntity.getGroupId() : null;
                String creator = docEntity != null ? docEntity.getCreator() : null;
                String groupName = null;
                if (groupId != null) {
                    com.xy.lucky.knowledge.domain.po.GroupPo g = groupRepository.findById(groupId).block();
                    if (g != null) {
                        groupName = g.getName();
                    }
                }
                for (Document d : documents) {
                    d.getMetadata().putAll(Map.of(
                            "docId", docId,
                            "filename", filename
                    ));
                    if (creator != null) d.getMetadata().put("creator", creator);
                    if (groupId != null) d.getMetadata().put("groupId", groupId);
                    if (groupName != null) d.getMetadata().put("groupName", groupName);
                }

                // 3. 文本切分
                TokenTextSplitter splitter = new TokenTextSplitter();
                List<Document> splitDocuments = splitter.apply(documents);

                // 4. 向量入库（Elasticsearch VectorStore 或其他后端）
                vectorStore.add(splitDocuments);

                // 5. 全文入库（Elasticsearch 文本索引）
                String contentText = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));
                EsKnowledgeDoc esDoc = new EsKnowledgeDoc()
                        .setId(docId + "-" + version)
                        .setDocId(docId)
                        .setVersion(version)
                        .setTitle(filename)
                        .setContent(contentText)
                        .setFilename(filename);
                documentRepository.findById(docId).subscribe(doc -> {
                    esDoc.setCreator(doc.getCreator())
                            .setCreateTime(doc.getCreateTime())
                            .setGroupId(doc.getGroupId());
                    Long gid = doc.getGroupId();
                    if (gid != null) {
                        groupRepository.findById(gid).subscribe(g -> {
                            if (g != null) {
                                esDoc.setGroupName(g.getName());
                            }
                            esSearchService.indexText(esDoc).subscribe();
                        }, err -> esSearchService.indexText(esDoc).subscribe(), () -> esSearchService.indexText(esDoc).subscribe());
                    } else {
                        esSearchService.indexText(esDoc).subscribe();
                    }
                });

                // 6. 更新文档状态为“已索引”
                documentRepository.findById(docId)
                        .flatMap(doc -> {
                            doc.setStatus(2);
                            return documentRepository.save(doc);
                        }).subscribe();

                log.info("Indexed document: {}", docId);

            } catch (Exception e) {
                log.error("Error indexing document: {}", docId, e);
                // 更新文档状态为“失败”
                documentRepository.findById(docId)
                        .flatMap(doc -> {
                            doc.setStatus(3);
                            return documentRepository.save(doc);
                        }).subscribe();
            }
        });
    }

    @Override
    public Mono<Void> deleteIndex(Long docId) {
        // TODO: VectorStore 删除通常需要向量ID；若后续支持按元数据删除可在此补充
        return Mono.empty();
    }

    @Override
    public Flux<String> search(String query, String creator) {
        return Mono.fromCallable(() ->
                        vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(5).build())
                ).flatMapMany(Flux::fromIterable)
                .map(Document::getText);
    }

    @Override
    public Mono<AiChatResponse> chat(String query, String creator) {
        return Mono.fromCallable(() -> {
            // 1. 语义检索相关片段
            List<Document> similarDocs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(3).build()
            );

            String context = similarDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));

            // 2. 构建系统提示词
            String systemText = """
                    You are a helpful assistant for the enterprise knowledge base.
                    Use the following pieces of context to answer the question at the end.
                    If you don't know the answer, just say that you don't know, don't try to make up an answer.
                    
                    Context:
                    {context}
                    """;

            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
            Prompt prompt = systemPromptTemplate.create(Map.of("context", context));

            // 3. 调用大模型生成答案
            ChatClient client = chatClientBuilder.build();
            String answer = client.prompt(prompt)
                    .user(query)
                    .call()
                    .content();

            AiChatResponse response = new AiChatResponse();
            response.setAnswer(answer);
            response.setSourceDocuments(similarDocs.stream()
                    .map(d -> (String) d.getMetadata().getOrDefault("filename", "Unknown"))
                    .distinct()
                    .collect(Collectors.toList()));

            return response;
        });
    }
}
