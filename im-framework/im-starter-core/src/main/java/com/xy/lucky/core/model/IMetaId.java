package com.xy.lucky.core.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IMetaId implements Serializable {

    /**
     * 元ID
     */
    private Object metaId;

    /**
     * 字符串ID
     */
    private String stringId;

    /**
     * 长整型ID
     */
    private Long longId;

}
