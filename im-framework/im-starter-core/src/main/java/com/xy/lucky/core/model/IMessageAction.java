package com.xy.lucky.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Map;

/**
 * 通用消息操作 DTO — 用于撤回/编辑等动作
 * <p>
 * 通过 actionType 区分操作；使用 Validation Groups 做字段校验。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IMessageAction extends IMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型：RECALL / EDIT
     */
    @NotNull(message = "actionType 不能为空")
    private Integer actionType;

    /**
     * 执行操作的用户（操作者）
     */
    @NotBlank(message = "operatorId 不能为空")
    private String operatorId;

    /**
     * 撤回时间（毫秒）——撤回时可选/默认由服务端填充
     */
    @NotNull(message = "recallTime 不能为空")
    private Long recallTime;

    /**
     * 撤回原因/备注（可选）
     */
    private String reason;

    /* ----------------- 编辑相关字段（EditGroup） ----------------- */

    /**
     * 编辑时间（毫秒）
     */
    @NotNull(message = "actionTime 不能为空")
    private Long actionTime;

    /**
     * 新的消息类型（例如文本/图片等），编辑时可指定以便客户端正确解析 newMessageBody。
     * 若不需要，可传 null 且 newMessageBody 里含类型字段。
     */
    private Integer newMessageContentType;

    /**
     * 编辑后的消息体（任意结构，建议为 map 或具体 messageBody json）
     * - 对文本编辑可为 { "text": "new text" }
     * - 对复杂类型可按业务传完整结构
     */
    @NotNull(message = "newMessageBody 不能为空")
    private Map<String, Object> newMessageBody;

    /* ----------------- 通用路由/展示辅助字段 ----------------- */

    /**
     * 目标会话 id（可选：用于路由/回推）
     */
    private String chatId;

    /**
     * message type
     */
    private Integer messageType;
}
