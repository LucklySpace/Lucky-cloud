package com.xy.lucky.knowledge.service.impl;

import com.xy.lucky.knowledge.domain.po.AuditLogPo;
import com.xy.lucky.knowledge.domain.po.DocumentPo;
import com.xy.lucky.knowledge.domain.po.DocumentVersionPo;
import com.xy.lucky.knowledge.domain.vo.DocumentVersionVo;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import com.xy.lucky.knowledge.mapper.DocumentMapper;
import com.xy.lucky.knowledge.repository.AuditLogRepository;
import com.xy.lucky.knowledge.repository.DocumentRepository;
import com.xy.lucky.knowledge.repository.DocumentVersionRepository;
import com.xy.lucky.knowledge.repository.GroupRepository;
import com.xy.lucky.knowledge.service.*;
import com.xy.lucky.knowledge.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final AuditLogRepository auditLogRepository;
    private final MinioUtils minioUtils;
    private final AiSearchService aiSearchService;
    private final EsSearchService esSearchService;
    private final RedisLockService redisLockService;
    private final HeatService heatService;
    private final CacheService cacheService;
    private final DocumentMapper mapper;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Mono<DocumentVo> upload(FilePart filePart, String creator, Long groupId) {
        return processUpload(filePart, creator, groupId, null);
    }

    @Override
    @Transactional
    public Mono<DocumentVo> update(Long docId, FilePart filePart, String creator, Long groupId) {
        return documentRepository.findById(docId)
                .switchIfEmpty(Mono.error(new RuntimeException("Document not found")))
                .flatMap(doc -> processUpload(filePart, creator, groupId, doc));
    }

    private Mono<DocumentVo> processUpload(FilePart filePart, String creator, Long groupId, DocumentPo existingDoc) {
        String filename = filePart.filename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String objectName = UUID.randomUUID() + extension;
        String contentType = filePart.headers().getContentType() != null ?
                filePart.headers().getContentType().toString() : "application/octet-stream";
        String lockKey = "lock:doc:" + (existingDoc == null ? filename : existingDoc.getId());
        String lockVal = UUID.randomUUID().toString();

        return redisLockService.acquireLock(lockKey, lockVal, java.time.Duration.ofSeconds(30))
                .flatMap(acquired -> {
                    if (!acquired) {
                        return Mono.error(new RuntimeException("操作过于频繁，请稍后再试"));
                    }
                    return DataBufferUtils.join(filePart.content());
                })
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> {
                    long size = bytes.length;
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

                    return minioUtils.upload(objectName, inputStream, size, contentType)
                            .then(Mono.defer(() -> {
                                if (existingDoc == null) {
                                    // 新增文档元数据
                                    DocumentPo doc = new DocumentPo()
                                            .setTitle(filename)
                                            .setOriginalFilename(filename)
                                            .setStoragePath(objectName)
                                            .setContentType(contentType)
                                            .setSize(size)
                                            .setStatus(1) // Processing
                                            .setVersion(1)
                                            .setCreateTime(LocalDateTime.now())
                                            .setUpdateTime(LocalDateTime.now())
                                            .setCreator(creator)
                                            .setGroupId(groupId);
                                    return documentRepository.save(doc);
                                } else {
                                    // 更新文档元数据（新增版本）
                                    existingDoc.setUpdateTime(LocalDateTime.now())
                                            .setVersion(existingDoc.getVersion() + 1)
                                            .setStoragePath(objectName) // Update latest path
                                            .setSize(size)
                                            .setStatus(1); // Processing again
                                    if (groupId != null) {
                                        existingDoc.setGroupId(groupId);
                                    }
                                    return documentRepository.save(existingDoc);
                                }
                            }))
                            .flatMap(savedDoc -> {
                                // 创建版本记录
                                DocumentVersionPo version = new DocumentVersionPo()
                                        .setDocumentId(savedDoc.getId())
                                        .setVersionNumber(savedDoc.getVersion())
                                        .setStoragePath(objectName)
                                        .setSize(size)
                                        .setCreateTime(LocalDateTime.now())
                                        .setCreator(creator);
                                return versionRepository.save(version).thenReturn(savedDoc);
                            })
                            .flatMap(savedDoc -> {
                                // 记录审计日志
                                String action = existingDoc == null ? "UPLOAD" : "UPDATE_VERSION";
                                AuditLogPo log = new AuditLogPo()
                                        .setDocumentId(savedDoc.getId())
                                        .setAction(action)
                                        .setOperator(creator)
                                        .setCreateTime(LocalDateTime.now())
                                        .setDetails("File: " + filename + ", Version: " + savedDoc.getVersion());
                                return auditLogRepository.save(log).thenReturn(savedDoc);
                            })
                            .flatMap(savedDoc -> {
                                // 异步索引：向量索引 + ES 全文索引
                                Mono.fromRunnable(() ->
                                        aiSearchService.indexDocument(savedDoc.getId(), savedDoc.getVersion(), bytes, filename)
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .subscribe()
                                ).subscribeOn(Schedulers.boundedElastic()).subscribe();
                                // ES 全文索引由 AiSearchService 调用 EsSearchService 完成

                                return enrichVo(savedDoc);
                            })
                            .doFinally(signal -> redisLockService.releaseLock(lockKey, lockVal).subscribe());
                });
    }

    @Override
    public Flux<DocumentVo> list(String creator) {
        return documentRepository.findByCreator(creator)
                .flatMap(this::enrichVo);
    }

    @Override
    public Mono<DocumentVo> getDetail(Long id, String requester) {
        return documentRepository.findById(id)
                .flatMap(doc -> {
                    if (!hasAccess(doc, requester)) {
                        return Mono.error(new RuntimeException("无权限访问该文档"));
                    }
                    return cacheService.getDocument(id)
                            .switchIfEmpty(enrichVo(doc)
                                    .flatMap(vo -> cacheService.setDocument(id, vo, java.time.Duration.ofMinutes(10)).thenReturn(vo)));
                });
    }

    @Override
    public Mono<Void> delete(Long id, String operator) {
        return documentRepository.findById(id)
                .flatMap(doc -> {
                    // 删除策略：先删除主文件与索引，版本文件可按需清理
                    return minioUtils.remove(doc.getStoragePath())
                            .then(aiSearchService.deleteIndex(id))
                            .then(esSearchService.deleteByDocId(id))
                            .then(documentRepository.delete(doc))
                            .then(auditLogRepository.save(new AuditLogPo()
                                    .setDocumentId(id)
                                    .setAction("DELETE")
                                    .setOperator(operator)
                                    .setCreateTime(LocalDateTime.now())
                                    .setDetails("Deleted document: " + doc.getTitle())));
                }).then();
    }

    @Override
    public Flux<DocumentVersionVo> listVersions(Long docId) {
        return versionRepository.findByDocumentId(docId)
                .map(mapper::toVo);
    }

    @Override
    public Mono<String> getDownloadUrl(Long id, Integer version, String requester) {
        if (version == null) {
            return documentRepository.findById(id)
                    .flatMap(doc -> {
                        if (!hasAccess(doc, requester)) {
                            return Mono.error(new RuntimeException("无权限下载该文档"));
                        }
                        return minioUtils.getPresignedUrl(doc.getStoragePath());
                    })
                    .doOnSuccess(url -> heatService.increaseHeat(id, 1).subscribe());
        } else {
            // Find specific version (need repository method)
            // Assuming versionRepository logic:
            return versionRepository.findByDocumentId(id)
                    .filter(v -> v.getVersionNumber().equals(version))
                    .next()
                    .flatMap(v -> documentRepository.findById(id)
                            .flatMap(doc -> {
                                if (!hasAccess(doc, requester)) {
                                    return Mono.error(new RuntimeException("无权限下载该文档"));
                                }
                                return minioUtils.getPresignedUrl(v.getStoragePath());
                            }))
                    .doOnSuccess(url -> heatService.increaseHeat(id, 1).subscribe());
        }
    }

    @Override
    public Mono<Long> reindexAll() {
        return documentRepository.findAll()
                .flatMap(doc -> minioUtils.getObjectBytes(doc.getStoragePath())
                        .flatMap(bytes -> aiSearchService.indexDocument(doc.getId(), doc.getVersion(), bytes, doc.getOriginalFilename()))
                        .thenReturn(1L)
                ).count();
    }

    private boolean hasAccess(DocumentPo doc, String requester) {
        if (doc.getCreator() != null && doc.getCreator().equals(requester)) {
            return true;
        }
        String perm = doc.getPermission();
        if (perm == null || perm.isEmpty()) {
            return false;
        }
        if ("public".equalsIgnoreCase(perm)) {
            return true;
        }
        if (perm.startsWith("shared:")) {
            String users = perm.substring("shared:".length());
            for (String u : users.split(",")) {
                if (requester != null && requester.equals(u.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Flux<DocumentVo> listAccessible(String requester) {
        return documentRepository.findAll()
                .filter(doc -> hasAccess(doc, requester))
                .flatMap(this::enrichVo);
    }

    @Override
    public Mono<Boolean> updatePermission(Long docId, String permission, String operator) {
        return documentRepository.findById(docId)
                .flatMap(doc -> {
                    if (doc.getCreator() != null && doc.getCreator().equals(operator)) {
                        doc.setPermission(permission);
                        return documentRepository.save(doc)
                                .then(auditLogRepository.save(new AuditLogPo()
                                        .setDocumentId(docId)
                                        .setAction("UPDATE_PERMISSION")
                                        .setOperator(operator)
                                        .setCreateTime(java.time.LocalDateTime.now())
                                        .setDetails("Permission: " + permission)))
                                .thenReturn(true);
                    }
                    return Mono.error(new RuntimeException("仅文档创建者可修改权限"));
                });
    }

    private Mono<DocumentVo> enrichVo(DocumentPo doc) {
        DocumentVo vo = mapper.toVo(doc);
        Long gid = doc.getGroupId();
        if (gid == null) {
            return Mono.just(vo);
        }
        return groupRepository.findById(gid)
                .map(g -> {
                    vo.setGroupName(g.getName());
                    return vo;
                })
                .defaultIfEmpty(vo);
    }
}
