package com.xy.lucky.ai.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 聊天会话实体类，对应 chat_session 表。
 * 表示一次对话历史记录，可配置模型、默认提示词等。
 */
@Entity
@Table(name = "chat_session")
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @Column(columnDefinition = "id")
    private String id;

    @Schema(description = "用户ID")
    @Column(name = "user_id")
    private String userId;

    @Schema(description = "会话标题")
    @Column(name = "title")
    private String title;

    @Schema(description = "默认系统提示词ID")
    @Column(name = "prompt_id")
    private String promptId;

//    @Schema(description = "使用的模型名称")
//    @Column(name = "model_name")
//    private String modelName;

    @Schema(description = "摘要，用于快速预览会话内容")
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Schema(description = "消息数量")
    @Column(name = "message_count")
    private Integer messageCount;

    @Schema(description = "会话状态，如 active / archived / deleted")
    @Column(name = "status")
    private String status;

    @Schema(description = "分类标签")
    @Column(name = "category")
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Schema(description = "最近一条消息的时间")
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Version
    @Column(name = "version")
    private Integer version;

    public ChatSession(String id) {
        this.id = id;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = "active";
        this.messageCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.lastMessageAt = LocalDateTime.now();
    }
}
