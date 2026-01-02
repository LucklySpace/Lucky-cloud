package com.xy.lucky.quartz.controller;

import com.xy.lucky.quartz.domain.dto.RegistryParam;
import com.xy.lucky.quartz.service.JobRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "执行器注册接口")
@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
public class JobRegistryController {

    private final JobRegistryService jobRegistryService;

    @Operation(summary = "注册执行器")
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistryParam param) {
        jobRegistryService.register(param);
        return ResponseEntity.ok("success");
    }

    @Operation(summary = "注销执行器")
    @PostMapping("/remove")
    public ResponseEntity<String> remove(@RequestBody RegistryParam param) {
        jobRegistryService.remove(param);
        return ResponseEntity.ok("success");
    }
}
