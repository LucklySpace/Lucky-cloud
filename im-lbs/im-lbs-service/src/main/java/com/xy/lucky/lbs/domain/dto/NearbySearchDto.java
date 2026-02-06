package com.xy.lucky.lbs.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "附近用户搜索请求")
public class NearbySearchDto {

    @Schema(description = "搜索半径(米)", example = "5000")
    private Double radius;

    @Schema(description = "最大结果数", example = "20")
    private Integer limit;

    @Schema(description = "分页页码", example = "1")
    private Integer page;
}
