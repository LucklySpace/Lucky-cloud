package com.xy.lucky.lbs.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_lbs_location_history")
@Schema(description = "用户位置历史记录")
public class LocationHistoryPo {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "GeoHash值")
    private String geohash;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(exist = false)
    @Schema(description = "距离(米)")
    private Double distance;
}
