package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.vo.TaskInfoVo;
import com.xy.lucky.quartz.mapper.TaskMapper;
import com.xy.lucky.quartz.service.TaskService;
import com.xy.lucky.quartz.repository.TaskLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final TaskService taskService;
    private final TaskLogRepository taskLogRepository;
    private final TaskMapper taskMapper;

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
    public String logs(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                       @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
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

    @org.springframework.web.bind.annotation.PostMapping("/tasks/save")
    public String saveTask(@jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("task") TaskInfoVo taskInfoVo,
                           org.springframework.validation.BindingResult bindingResult,
                           Model model) throws org.quartz.SchedulerException {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", taskInfoVo.getId() == null);
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

    @org.springframework.web.bind.annotation.PostMapping("/tasks/{id}/delete")
    public String deleteTaskAction(@PathVariable Long id) throws org.quartz.SchedulerException {
        taskService.deleteTask(id);
        return "redirect:/tasks";
    }

    @org.springframework.web.bind.annotation.PostMapping("/tasks/{id}/start")
    public String startTaskAction(@PathVariable Long id) throws org.quartz.SchedulerException {
        taskService.startTask(id);
        return "redirect:/tasks";
    }

    @org.springframework.web.bind.annotation.PostMapping("/tasks/{id}/stop")
    public String stopTaskAction(@PathVariable Long id) throws org.quartz.SchedulerException {
        taskService.stopTask(id);
        return "redirect:/tasks";
    }

    @org.springframework.web.bind.annotation.PostMapping("/tasks/{id}/trigger")
    public String triggerTaskAction(@PathVariable Long id) throws org.quartz.SchedulerException {
        taskService.triggerTask(id);
        return "redirect:/tasks";
    }
}
