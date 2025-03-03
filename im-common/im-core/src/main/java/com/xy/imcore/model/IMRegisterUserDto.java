package com.xy.imcore.model;


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
public class IMRegisterUserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;

    private String token;

    private String brokerId;


}
