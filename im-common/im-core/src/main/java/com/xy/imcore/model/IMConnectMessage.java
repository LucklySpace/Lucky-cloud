package com.xy.imcore.model;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;


/**
 * websocket connection info
 *
 * @param <T>
 */
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonSubTypes({
        @JsonSubTypes.Type(value = String.class),
        // 你可以根据需要添加更多的子类型
        // @JsonSubTypes.Type(value = AnotherClass.class, name = "anotherClass")
})
public class IMConnectMessage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * connection type
     */
    private Integer code;

    /**
     * connection user's token
     */
    private String token;

    /**
     * connection information
     */
    private T data;

}
