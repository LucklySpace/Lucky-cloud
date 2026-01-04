package com.xy.lucky.knowledge.service.impl;

import com.xy.lucky.knowledge.domain.po.AuditLogPo;
import com.xy.lucky.knowledge.domain.po.GroupPo;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import com.xy.lucky.knowledge.mapper.DocumentMapper;
import com.xy.lucky.knowledge.repository.AuditLogRepository;
import com.xy.lucky.knowledge.repository.DocumentRepository;
import com.xy.lucky.knowledge.repository.GroupRepository;
import com.xy.lucky.knowledge.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final DocumentMapper documentMapper;

    /**
     * 创建分组
     */
    @Override
    public Mono<?> createGroup(String owner, String name, String description) {
        return groupRepository.findByOwnerAndName(owner, name)
                .flatMap(existing -> Mono.error(new RuntimeException("分组已存在")))
                .switchIfEmpty(Mono.defer(() -> {
                    GroupPo g = new GroupPo()
                            .setOwner(owner)
                            .setName(name)
                            .setDescription(description)
                            .setCreateTime(LocalDateTime.now());
                    return groupRepository.save(g);
                }));
    }

    /**
     * 查询用户的分组列表
     */
    @Override
    public Flux<GroupPo> listGroups(String owner) {
        return groupRepository.findByOwner(owner);
    }

    /**
     * 将文档加入分组
     */
    @Override
    public Mono<Boolean> assignDocument(Long docId, Long groupId, String operator) {
        return documentRepository.findById(docId)
                .switchIfEmpty(Mono.error(new RuntimeException("文档不存在")))
                .flatMap(doc -> groupRepository.findById(groupId)
                        .switchIfEmpty(Mono.error(new RuntimeException("分组不存在")))
                        .flatMap(g -> {
                            doc.setGroupId(groupId);
                            return documentRepository.save(doc)
                                    .then(auditLogRepository.save(new AuditLogPo()
                                            .setDocumentId(docId)
                                            .setAction("ASSIGN_GROUP")
                                            .setOperator(operator)
                                            .setCreateTime(LocalDateTime.now())
                                            .setDetails("GroupId: " + groupId + ", GroupName: " + g.getName())))
                                    .thenReturn(true);
                        }));
    }

    /**
     * 将文档移出分组
     */
    @Override
    public Mono<Boolean> removeDocument(Long docId, String operator) {
        return documentRepository.findById(docId)
                .switchIfEmpty(Mono.error(new RuntimeException("文档不存在")))
                .flatMap(doc -> {
                    doc.setGroupId(null);
                    return documentRepository.save(doc)
                            .then(auditLogRepository.save(new AuditLogPo()
                                    .setDocumentId(docId)
                                    .setAction("REMOVE_GROUP")
                                    .setOperator(operator)
                                    .setCreateTime(LocalDateTime.now())
                                    .setDetails("Remove group from doc")))
                            .thenReturn(true);
                });
    }

    /**
     * 查询分组下的文档
     */
    @Override
    public Flux<DocumentVo> listDocuments(Long groupId) {
        return documentRepository.findByGroupId(groupId)
                .map(documentMapper::toVo);
    }
}
