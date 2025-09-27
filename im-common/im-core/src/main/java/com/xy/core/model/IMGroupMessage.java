package com.xy.core.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.core.enums.IMessageType;
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
 * group of chat messages
 */
@Data
@SuperBuilder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class IMGroupMessage extends IMessage implements Serializable {

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
    private Integer messageType = IMessageType.GROUP_MESSAGE.getCode();

}
