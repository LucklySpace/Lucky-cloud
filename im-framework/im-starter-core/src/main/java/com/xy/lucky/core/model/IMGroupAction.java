package com.xy.lucky.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.lucky.core.enums.IMessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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
public class IMGroupAction extends IMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * group id
     */
    @NotBlank(message = "群聊id不能为空")
    private String groupId;

    /**
     * group members
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> toList;

    /**
     * message type
     */
    private Integer messageType = IMessageType.GROUP_OPERATION.getCode();
}
