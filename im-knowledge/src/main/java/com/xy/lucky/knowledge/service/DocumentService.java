package com.xy.lucky.knowledge.service;

import com.xy.lucky.knowledge.domain.vo.DocumentVersionVo;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentService {

    /**
     * 上传新文档
     */
    Mono<DocumentVo> upload(FilePart filePart, String creator, Long groupId);

    /**
     * 更新文档（新增版本）
     */
    Mono<DocumentVo> update(Long docId, FilePart filePart, String creator, Long groupId);

    /**
     * 获取文档列表
     */
    Flux<DocumentVo> list(String creator);

    /**
     * 获取文档详情
     */
    Mono<DocumentVo> getDetail(Long id, String requester);

    /**
     * 删除文档
     */
    Mono<Void> delete(Long id, String operator);

    /**
     * 获取版本历史
     */
    Flux<DocumentVersionVo> listVersions(Long docId);

    /**
     * 获取下载链接
     */
    Mono<String> getDownloadUrl(Long id, Integer version, String requester);

    /**
     * 批量重建索引（全文 + 向量）
     */
    Mono<Long> reindexAll();

    /**
     * 获取当前用户可访问的文档列表
     */
    Flux<DocumentVo> listAccessible(String requester);

    /**
     * 更新文档权限
     */
    Mono<Boolean> updatePermission(Long docId, String permission, String operator);
}
