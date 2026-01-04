package com.xy.lucky.knowledge.controller;

import com.xy.lucky.knowledge.domain.po.GroupPo;
import com.xy.lucky.knowledge.domain.vo.AiChatResponse;
import com.xy.lucky.knowledge.domain.vo.DocumentVersionVo;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import com.xy.lucky.knowledge.service.AiSearchService;
import com.xy.lucky.knowledge.service.DocumentService;
import com.xy.lucky.knowledge.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
@Tag(name = "knowledge", description = "知识库管理接口")
public class KnowledgeController {

    private final DocumentService documentService;
    private final AiSearchService aiSearchService;
    private final com.xy.lucky.knowledge.service.EsSearchService esSearchService;
    private final com.xy.lucky.knowledge.service.HeatService heatService;
    private final GroupService groupService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传文档并自动解析、向量化")
    public Mono<DocumentVo> upload(
            @RequestPart("file") FilePart file,
            @Parameter(description = "创建人") @RequestParam("creator") String creator,
            @Parameter(description = "分组ID，可选") @RequestParam(value = "groupId", required = false) Long groupId) {
        return documentService.upload(file, creator, groupId);
    }

    @PostMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更新文档", description = "上传新版本文档")
    public Mono<DocumentVo> update(
            @PathVariable Long id,
            @RequestPart("file") FilePart file,
            @Parameter(description = "操作人") @RequestParam("creator") String creator,
            @Parameter(description = "分组ID，可选") @RequestParam(value = "groupId", required = false) Long groupId) {
        return documentService.update(id, file, creator, groupId);
    }

    @GetMapping("/list")
    @Operation(summary = "文档列表", description = "查询指定用户的文档列表")
    public Flux<DocumentVo> list(@RequestParam("creator") String creator) {
        return documentService.list(creator);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "文档详情", description = "获取文档详细信息")
    public Mono<DocumentVo> detail(@PathVariable Long id,
                                   @Parameter(description = "访问人") @RequestParam("requester") String requester) {
        return documentService.getDetail(id, requester);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "删除文档及相关存储")
    public Mono<Void> delete(@PathVariable Long id, @RequestParam("operator") String operator) {
        return documentService.delete(id, operator);
    }

    @GetMapping("/versions/{id}")
    @Operation(summary = "版本历史", description = "获取文档的版本历史")
    public Flux<DocumentVersionVo> listVersions(@PathVariable Long id) {
        return documentService.listVersions(id);
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "获取下载链接", description = "获取文档的临时下载链接")
    public Mono<String> getDownloadUrl(
            @PathVariable Long id,
            @RequestParam(value = "version", required = false) Integer version,
            @Parameter(description = "访问人") @RequestParam("requester") String requester) {
        return documentService.getDownloadUrl(id, version, requester);
    }

    @PostMapping("/reindex")
    @Operation(summary = "批量重建索引", description = "重建全部文档的全文与向量索引")
    public Mono<Long> reindex() {
        return documentService.reindexAll();
    }

    // --- AI & Search ---

    @GetMapping("/search")
    @Operation(summary = "语义检索", description = "基于向量的语义检索")
    public Flux<String> search(
            @RequestParam("query") String query,
            @RequestParam(value = "creator", required = false) String creator) {
        return aiSearchService.search(query, creator);
    }

    @GetMapping("/search/text")
    @Operation(summary = "全文检索", description = "基于 Elasticsearch 的全文检索")
    public Flux<com.xy.lucky.knowledge.domain.es.EsKnowledgeDoc> searchText(
            @RequestParam("query") String query,
            @RequestParam(value = "creator", required = false) String creator,
            @Parameter(description = "分组ID，可选") @RequestParam(value = "groupId", required = false) Long groupId) {
        return esSearchService.searchText(query, creator, groupId);
    }

    @GetMapping("/hot")
    @Operation(summary = "热门文档", description = "获取热度最高的文档列表（默认Top 10）")
    public Flux<DocumentVo> hot(@RequestParam(value = "top", defaultValue = "10") int top,
                                @Parameter(description = "访问人") @RequestParam("requester") String requester) {
        return heatService.topHotDocs(top)
                .flatMap(id -> documentService.getDetail(id, requester));
    }

    @GetMapping("/list-accessible")
    @Operation(summary = "可访问文档列表", description = "获取当前用户可访问的文档列表")
    public Flux<DocumentVo> listAccessible(@Parameter(description = "访问人") @RequestParam("requester") String requester) {
        return documentService.listAccessible(requester);
    }

    @PutMapping("/permission/{id}")
    @Operation(summary = "更新文档权限", description = "仅文档创建者可修改权限")
    public Mono<Boolean> updatePermission(@PathVariable Long id,
                                          @Parameter(description = "权限：public/shared:user1,user2/private") @RequestParam("permission") String permission,
                                          @Parameter(description = "操作人") @RequestParam("operator") String operator) {
        return documentService.updatePermission(id, permission, operator);
    }

    @GetMapping("/chat")
    @Operation(summary = "AI问答", description = "基于知识库的问答")
    public Mono<AiChatResponse> chat(
            @RequestParam("query") String query,
            @RequestParam(value = "creator", required = false) String creator) {
        return aiSearchService.chat(query, creator);
    }

    @PostMapping("/group/create")
    @Operation(summary = "创建分组", description = "创建文档分组")
    public Mono<GroupPo> createGroup(@RequestParam("owner") String owner,
                                     @RequestParam("name") String name,
                                     @RequestParam(value = "description", required = false) String description) {
        return groupService.createGroup(owner, name, description);
    }

    @GetMapping("/group/list")
    @Operation(summary = "分组列表", description = "查询用户的分组列表")
    public Flux<GroupPo> listGroups(@RequestParam("owner") String owner) {
        return groupService.listGroups(owner);
    }

    @PutMapping("/group/assign/{docId}")
    @Operation(summary = "分组分配", description = "将文档加入指定分组")
    public Mono<Boolean> assignDocument(@PathVariable Long docId,
                                        @RequestParam("groupId") Long groupId,
                                        @RequestParam("operator") String operator) {
        return groupService.assignDocument(docId, groupId, operator);
    }

    @PutMapping("/group/remove/{docId}")
    @Operation(summary = "移出分组", description = "将文档移出分组")
    public Mono<Boolean> removeDocument(@PathVariable Long docId,
                                        @RequestParam("operator") String operator) {
        return groupService.removeDocument(docId, operator);
    }

    @GetMapping("/group/{groupId}/documents")
    @Operation(summary = "分组文档", description = "查询分组下的文档")
    public Flux<DocumentVo> listGroupDocuments(@PathVariable Long groupId) {
        return groupService.listDocuments(groupId);
    }
}
