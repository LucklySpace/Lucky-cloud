package com.xy.lucky.rpc.api.lbs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "行政区划信息")
public class RegionVo {

    @Schema(description = "行政区划代码")
    private Long code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "完整地址")
    private String fullAddress;

    @Schema(description = "行政级别(1:省, 2:市, 3:区县, 4:乡镇, 5:村)")
    private Integer level;
}
