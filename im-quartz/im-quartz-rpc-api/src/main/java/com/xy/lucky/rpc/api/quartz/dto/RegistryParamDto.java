package com.xy.lucky.rpc.api.quartz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 注册参数 DTO
 *
 * @author Lucky
 */
@Data
@Schema(description = "注册参数DTO")
public class RegistryParamDto implements Serializable {

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "执行器地址 ip:port")
    private String address;

    @Schema(description = "任务列表")
    private List<JobInfoDto> jobs;

    @Data
    @Schema(description = "任务信息")
    public static class JobInfoDto implements Serializable {

        @Schema(description = "任务名称")
        private String name;

        @Schema(description = "任务描述")
        private String description;

        @Schema(description = "初始化参数")
        private String initParams;
    }
}
