package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.vo.TaskInfoVo;
import com.xy.lucky.quartz.mapper.TaskMapper;
import com.xy.lucky.quartz.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "分布式定时任务管理接口")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @GetMapping
    @Operation(summary = "获取所有任务列表")
    public ResponseEntity<List<TaskInfoVo>> listTasks() {
        List<TaskInfoPo> tasks = taskService.findAll();
        return ResponseEntity.ok(taskMapper.toTaskInfoVoList(tasks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public ResponseEntity<TaskInfoVo> getTask(@PathVariable Long id) {
        TaskInfoPo task = taskService.findById(id);
        return ResponseEntity.ok(taskMapper.toVo(task));
    }

    @PostMapping
    @Operation(summary = "创建新任务")
    public ResponseEntity<Void> createTask(@jakarta.validation.Valid @RequestBody TaskInfoVo taskInfoVo) {
        TaskInfoPo taskInfoPo = taskMapper.toEntity(taskInfoVo);
        taskService.addTask(taskInfoPo);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    public ResponseEntity<Void> updateTask(@PathVariable Long id, @jakarta.validation.Valid @RequestBody TaskInfoVo taskInfoVo) throws SchedulerException {
        taskInfoVo.setId(id);
        TaskInfoPo taskInfoPo = taskMapper.toEntity(taskInfoVo);
        taskService.updateTask(taskInfoPo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) throws SchedulerException {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "启动任务")
    public ResponseEntity<Void> startTask(@PathVariable Long id) throws SchedulerException {
        taskService.startTask(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "停止任务")
    public ResponseEntity<Void> stopTask(@PathVariable Long id) throws SchedulerException {
        taskService.stopTask(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "立即执行一次任务")
    public ResponseEntity<Void> triggerTask(@PathVariable Long id) throws SchedulerException {
        taskService.triggerTask(id);
        return ResponseEntity.ok().build();
    }
}
