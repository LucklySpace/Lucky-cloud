package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.vo.TaskInfoVo;
import com.xy.lucky.quartz.mapper.TaskMapper;
import com.xy.lucky.quartz.repository.TaskLogRepository;
import com.xy.lucky.quartz.service.JobRegistryService;
import com.xy.lucky.quartz.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final TaskService taskService;
    private final TaskLogRepository taskLogRepository;
    private final TaskMapper taskMapper;
    private final JobRegistryService jobRegistryService;

    @GetMapping("/")
    public String index() {
        return "redirect:/tasks";
    }

    @GetMapping("/tasks")
    public String tasks(Model model) {
        model.addAttribute("tasks", taskMapper.toTaskInfoVoList(taskService.findAll()));
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTask(Model model) {
        model.addAttribute("task", new TaskInfoVo());
        model.addAttribute("isNew", true);
        return "task_form";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTask(@PathVariable Long id, Model model) {
        TaskInfoPo task = taskService.findById(id);
        if (task == null) {
            return "redirect:/tasks";
        }
        model.addAttribute("task", taskMapper.toVo(task));
        model.addAttribute("isNew", false);
        return "task_form";
    }

    @GetMapping("/tasks/{id}")
    public String taskDetail(@PathVariable Long id, Model model) {
        TaskInfoPo task = taskService.findById(id);
        if (task == null) {
            return "redirect:/tasks";
        }
        model.addAttribute("task", taskMapper.toVo(task));
        model.addAttribute("logs", taskLogRepository.findByJobName(task.getJobName(),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startTime")))
                .map(taskMapper::toVo));
        return "task_detail";
    }

    @GetMapping("/logs")
    public String logs(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("logs", taskLogRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime")))
                .map(taskMapper::toVo));
        return "logs";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Form Actions

    @PostMapping("/tasks/save")
    public String saveTask(@Valid @ModelAttribute("task") TaskInfoVo taskInfoVo,
                           BindingResult bindingResult,
                           Model model) throws SchedulerException {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", taskInfoVo.getId() == null);
            model.addAttribute("appNames", jobRegistryService.getAllServices());
            return "task_form";
        }

        TaskInfoPo taskInfoPo = taskMapper.toEntity(taskInfoVo);
        if (taskInfoPo.getId() == null) {
            taskService.addTask(taskInfoPo);
        } else {
            taskService.updateTask(taskInfoPo);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTaskAction(@PathVariable Long id) throws SchedulerException {
        taskService.deleteTask(id);
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/start")
    public String startTaskAction(@PathVariable Long id) throws SchedulerException {
        taskService.startTask(id);
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/stop")
    public String stopTaskAction(@PathVariable Long id) throws SchedulerException {
        taskService.stopTask(id);
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/trigger")
    public String triggerTaskAction(@PathVariable Long id) throws SchedulerException {
        taskService.triggerTask(id);
        return "redirect:/tasks";
    }
}
