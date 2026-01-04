package com.xy.lucky.knowledge.service;

import com.xy.lucky.knowledge.domain.po.GroupPo;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GroupService {
    /**
     * 创建分组
     */
    Mono<?> createGroup(String owner, String name, String description);

    /**
     * 查询用户的分组列表
     */
    Flux<GroupPo> listGroups(String owner);

    /**
     * 将文档加入分组
     */
    Mono<Boolean> assignDocument(Long docId, Long groupId, String operator);

    /**
     * 将文档移出分组
     */
    Mono<Boolean> removeDocument(Long docId, String operator);

    /**
     * 查询分组下的文档
     */
    Flux<DocumentVo> listDocuments(Long groupId);
}
