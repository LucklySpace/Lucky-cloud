package com.xy.lucky.rpc.api.lbs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "完整地址信息")
public class AddressVo {

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "乡镇")
    private String town;

    @Schema(description = "村/社区")
    private String village;

    @Schema(description = "完整地址")
    private String fullAddress;

    @Schema(description = "最细粒度行政区划代码")
    private Long adCode;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;
}
