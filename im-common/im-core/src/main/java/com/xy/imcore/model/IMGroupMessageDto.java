package com.xy.imcore.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.imcore.enums.IMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * group of chat messages
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class IMGroupMessageDto extends IMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * group id
     */
    private String groupId;

    /**
     * group members
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> to_List;

    /**
     * message type
     */
    private Integer messageType = IMessageType.GROUP_MESSAGE.getCode();

}
