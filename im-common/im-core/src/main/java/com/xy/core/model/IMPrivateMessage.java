package com.xy.core.model;


import com.xy.core.enums.IMessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * single of chat messages
 *
 * @author dense
 */
@Data
@SuperBuilder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class IMPrivateMessage extends IMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * send to somebody
     */
    @NotBlank(message = "接收人id不能为空")
    private String toId;

    /**
     * message type
     */
    private Integer messageType = IMessageType.SINGLE_MESSAGE.getCode();

}
