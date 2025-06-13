package com.xy.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipRequestDto implements Serializable {

    private String id;

    private String fromId;

    private String toId;

    private String remark;

    private Integer approveStatus;

}