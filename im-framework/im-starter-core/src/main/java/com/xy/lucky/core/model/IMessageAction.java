package com.xy.lucky.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.lucky.core.enums.IMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

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

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 单聊场景：接收人ID
     */
    private String toId;

    /**
     * 群聊场景：群组ID
     */
    private String groupId;

    /**
     * 消息类型
     */
    private Integer messageType = IMessageType.MESSAGE_OPERATION.getCode();
}
