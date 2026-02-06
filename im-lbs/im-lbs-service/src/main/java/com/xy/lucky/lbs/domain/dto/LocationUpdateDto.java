package com.xy.lucky.lbs.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "位置上报请求")
public class LocationUpdateDto {

    @Schema(description = "经度", example = "116.397128")
    private Double longitude;

    @Schema(description = "纬度", example = "39.916527")
    private Double latitude;

}
