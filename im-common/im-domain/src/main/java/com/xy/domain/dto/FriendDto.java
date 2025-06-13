package com.xy.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友对象")
public class FriendDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自己id")
    private String fromId;

    @Schema(description = "好友id")
    private String toId;
}
