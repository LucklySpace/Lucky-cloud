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

    private Object metaId;

    private String stringId;

    private Long longId;

}
