package com.xy.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class IMVideoMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String fromId;

    private String toId;

    private String url;

    private Integer type;
}
