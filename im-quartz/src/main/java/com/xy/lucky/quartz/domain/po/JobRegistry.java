package com.xy.lucky.quartz.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_quartz_job_registry")
@Schema(description = "执行器注册信息")
public class JobRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "应用名称")
    private String appName;

    @Column(nullable = false)
    @Schema(description = "执行器地址")
    private String address;

    @Column(nullable = false)
    @Schema(description = "状态：0-离线，1-在线")
    private Integer status;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
