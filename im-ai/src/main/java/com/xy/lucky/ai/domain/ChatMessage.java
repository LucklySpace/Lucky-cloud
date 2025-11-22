package com.xy.lucky.ai.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


@Entity
@Table(name = "chat_message")
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChatMessage", description = "聊天消息实体，记录用户提问、AI 回复、系统提示等内容")
public class ChatMessage {

    @Id
    @Column(columnDefinition = "id")
    @Schema(description = "消息主键 ID，通常使用 UUID")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    @Schema(description = "所属的对话会话对象")
    private ChatSession session;

    @Column(name = "type")
    @Schema(description = "消息类型，如 user / assistant / system")
    private String type;

    @Column(name = "content", columnDefinition = "TEXT")
    @Schema(description = "消息正文内容，如提问文本、AI 回复等")
    private String content;

    @Column(name = "prompt_id")
    @Schema(description = "关联的 Prompt ID，表示该消息使用的提示模板，可为空")
    private String promptId;

    @Column(name = "streamed")
    @Schema(description = "是否为流式生成（如 SSE 等），true 表示是")
    private Boolean streamed;

    @Column(name = "parent_id")
    @Schema(description = "父消息 ID，用于多轮对话上下文引用，null 表示对话起点")
    private String parentId;

    @Version
    @Column(name = "version")
    @Schema(description = "乐观锁版本号")
    private Integer version;

    //    @Schema(description = "使用的模型名称")
//    @Column(name = "model_name")
//    private String modelName;

    @Column(name = "created_at")
    @Schema(description = "消息创建时间")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
