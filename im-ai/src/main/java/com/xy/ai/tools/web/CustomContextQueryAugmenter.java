//package com.xy.ai.tools.web;
//
//import org.springframework.ai.chat.prompt.PromptTemplate;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.rag.Query;
//import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//public class CustomContextQueryAugmenter implements QueryAugmenter {
//
//    private final PromptTemplate tmpl = new PromptTemplate(
//            "请结合以下网页摘要，优化用户查询：\n" +
//                    "原查询：{{query}}\n" +
//                    "摘要：\n{{context}}"
//    );
//
//    @Override
//    public Query augment(Query query, List<Document> docs) {
//        String ctx = docs.stream()
//                .map(Document::getText)
//                .collect(Collectors.joining("\n---\n"));
//
//        Map<String, Object> promptParameters = Map.of(
//                "query", query.text(),
//                "context", ctx
//        );
//        return new Query(this.tmpl.render(promptParameters));
//    }
//}