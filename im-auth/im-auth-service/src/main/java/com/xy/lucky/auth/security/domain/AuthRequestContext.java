package com.xy.lucky.auth.security.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestContext {
    private String clientIp;
    private String deviceId;
}

