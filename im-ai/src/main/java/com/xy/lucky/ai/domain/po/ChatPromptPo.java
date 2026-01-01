package com.xy.lucky.ai.domain.po;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_prompt")
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChatPrompt", description = "Prompt 配置实体，用于保存系统提示词或输入模板")
public class ChatPromptPo {

    @Id
    @Column(name = "id", length = 64)
    @Schema(description = "Prompt 唯一标识 ID，通常为 UUID")
    private String id;

    @Column(name = "name")
    @Schema(description = "Prompt 名称，用于界面展示或搜索")
    private String name;

    @Column(name = "prompt", columnDefinition = "TEXT")
    @Schema(description = "Prompt 内容，可为系统提示语、用户输入模板等")
    private String prompt;

    @Column(name = "description")
    @Schema(description = "Prompt 描述，用于说明用途、适用场景等")
    private String description;

    @Column(name = "created_at")
    @Schema(description = "创建时间，由系统自动设置")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "更新时间，由系统自动维护")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    @Schema(description = "版本号，用于乐观锁控制")
    private Integer version;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

