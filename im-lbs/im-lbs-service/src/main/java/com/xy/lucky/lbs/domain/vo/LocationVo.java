package com.xy.lucky.lbs.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "用户位置信息")
public class LocationVo {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "距离(米)")
    private Double distance;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;
}
