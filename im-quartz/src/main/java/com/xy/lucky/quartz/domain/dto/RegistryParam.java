package com.xy.lucky.quartz.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RegistryParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 执行器地址 ip:port
     */
    private String address;

    /**
     * 任务列表
     */
    private List<JobInfo> jobs;

    @Data
    public static class JobInfo implements Serializable {
        private String name;
        private String description;
        private String initParams;
    }
}
