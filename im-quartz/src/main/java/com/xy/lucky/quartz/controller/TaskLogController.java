package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.domain.po.TaskLogPo;
import com.xy.lucky.quartz.domain.vo.TaskLogVo;
import com.xy.lucky.quartz.mapper.TaskMapper;
import com.xy.lucky.quartz.repository.TaskLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "日志管理", description = "任务执行日志接口")
public class TaskLogController {

    private final TaskLogRepository taskLogRepository;
    private final TaskMapper taskMapper;

    @GetMapping
    @Operation(summary = "获取执行日志")
    public ResponseEntity<Page<TaskLogVo>> getLogs(
            @RequestParam(required = false) String jobName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        Page<TaskLogPo> logPage;
        if (jobName != null && !jobName.isEmpty()) {
            logPage = taskLogRepository.findByJobName(jobName, pageRequest);
        } else {
            logPage = taskLogRepository.findAll(pageRequest);
        }

        return ResponseEntity.ok(logPage.map(taskMapper::toVo));
    }
}
