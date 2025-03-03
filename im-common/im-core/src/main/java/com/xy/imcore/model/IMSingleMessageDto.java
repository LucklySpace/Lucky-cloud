package com.xy.imcore.model;


import com.xy.imcore.enums.IMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * single of chat messages
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class IMSingleMessageDto extends IMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * send to somebody
     */
    private String toId;

    /**
     * message type
     */
    private Integer messageType = IMessageType.SINGLE_MESSAGE.getCode();

}
