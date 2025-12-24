package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "AreaVo", description = "区域")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaVo {

    @Schema(description = "区域编号")
    private Integer code;

    @Schema(description = "IP 地址")
    private String ip;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "格式化后的完整路径，如：中国 / 上海 / 上海市 / 静安区")
    private String address;
}
