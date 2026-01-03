package com.xy.lucky.lbs.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("im_lbs_china_town")
@Schema(description = "乡镇信息")
public class TownPo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "区县代码")
    private Long countyCode;

    @Schema(description = "行政区划代码")
    private Long code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "纬度")
    private Double lat;

    @Schema(description = "经度")
    private Double lng;
}
