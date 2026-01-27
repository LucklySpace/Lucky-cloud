package com.xy.lucky.server.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDto {

    private String id;

    private String fromId;

    private String toId;

    private String remark;

    private String message;

    private Integer approveStatus;
}
