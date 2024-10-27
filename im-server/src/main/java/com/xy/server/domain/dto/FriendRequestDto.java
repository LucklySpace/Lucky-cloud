package com.xy.server.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor

public class FriendRequestDto {

    private String from_id;

    private String to_id;

    private String remark;

    private String add_wording;
}
