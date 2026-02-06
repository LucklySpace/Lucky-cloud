package com.xy.lucky.rpc.api.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 地区信息 VO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "AreaVo", description = "区域信息")
public class AreaVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "区域编号")
    private Integer code;

    @Schema(description = "IP 地址")
    private String ip;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "格式化后的完整路径，如：中国 / 上海 / 上海市 / 静安区")
    private String address;
}
