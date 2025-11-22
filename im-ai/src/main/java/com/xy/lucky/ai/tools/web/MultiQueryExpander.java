//package com.xy.ai.tools.web;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.prompt.PromptTemplate;
//import org.springframework.ai.rag.Query;
//import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
//import org.springframework.util.StringUtils;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class MultiQueryExpander implements QueryExpander {
//    private final ChatClient chatClient;
//    private final PromptTemplate tmpl;
//    private final int count;
//
//    public MultiQueryExpander(ChatClient client, PromptTemplate tmpl, int count) {
//        this.chatClient  = client;
//        this.tmpl        = tmpl;
//        this.count       = count;
//    }
//
//    @Override
//    public List<Query> expand(Query query) {
//        String resp = chatClient.prompt()
//                .user(u -> u.text(tmpl.getTemplate())
//                        .param("query", query.text())
//                        .param("num", count))
//                .call()
//                .content();
//        List<String> variants = Arrays.stream(resp.split("\n"))
//                .filter(StringUtils::hasText).toList();
//        return variants.stream()
//                .map(q -> query.mutate().text(q).build())
//                .collect(Collectors.toList());
//    }
//}
