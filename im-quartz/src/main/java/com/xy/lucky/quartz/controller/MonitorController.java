package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.manager.ClusterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ClusterManager clusterManager;

    @GetMapping
    public String monitor(Model model) {
        return "monitor";
    }

    @GetMapping("/cluster-info")
    @ResponseBody
    public Map<String, Object> getClusterInfo() {
        int[] sharding = clusterManager.getShardingInfo();
        Map<String, Object> info = new HashMap<>();
        info.put("shardingItem", sharding[0]);
        info.put("shardingTotal", sharding[1]);

        List<Map<String, Object>> instances = clusterManager.getInstances().stream()
                .map(ins -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("instanceId", ins.getInstanceId());
                    m.put("host", ins.getHost());
                    m.put("port", ins.getPort());
                    m.put("serviceId", ins.getServiceId());
                    return m;
                })
                .collect(Collectors.toList());

        info.put("instances", instances);
        info.put("serverTime", LocalDateTime.now());

        return info;
    }
}
