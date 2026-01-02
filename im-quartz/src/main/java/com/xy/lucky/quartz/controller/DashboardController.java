package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.domain.enums.TaskStatus;
import com.xy.lucky.quartz.repository.TaskInfoRepository;
import com.xy.lucky.quartz.repository.TaskLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TaskInfoRepository taskInfoRepository;
    private final TaskLogRepository taskLogRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalTasks = taskInfoRepository.count();
        long runningTasks = taskInfoRepository.countByStatus(TaskStatus.RUNNING);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayLogs = taskLogRepository.countByStartTimeAfter(todayStart);
        long todaySuccess = taskLogRepository.countByStartTimeAfterAndStatus(todayStart, 1);
        long todayFail = taskLogRepository.countByStartTimeAfterAndStatus(todayStart, 2);

        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("runningTasks", runningTasks);
        model.addAttribute("stoppedTasks", totalTasks - runningTasks);
        model.addAttribute("todayLogs", todayLogs);
        model.addAttribute("todaySuccess", todaySuccess);
        model.addAttribute("todayFail", todayFail);

        return "dashboard";
    }
}
