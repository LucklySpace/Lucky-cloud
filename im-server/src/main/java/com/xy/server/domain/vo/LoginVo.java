package com.xy.server.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class LoginVo implements Serializable {

    private static final long serialVersionUID = 1L;
    private String user_id;
    private String token;
}
