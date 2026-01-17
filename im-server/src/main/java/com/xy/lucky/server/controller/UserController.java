package com.xy.lucky.server.controller;


import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Executor;


/**
 * 用户
 */
@Slf4j
@RestController
@RequestMapping({"/api/user", "/api/{version}/user"})
@Tag(name = "user", description = "用户")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @PostMapping("/list")
    @Operation(summary = "查询用户列表", tags = {"user"}, description = "请使用此接口查找用户列表")
    @Parameters({
            @Parameter(name = "userDto", description = "用户信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<List<UserVo>> list(@RequestBody UserDto userDto) {
        return Mono.fromCallable(() -> userService.list(userDto))
                .subscribeOn(getScheduler());
    }

    @GetMapping("/one")
    @Operation(summary = "查询用户信息", tags = {"user"}, description = "请使用此接口获取用户信息")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<UserVo> one(@RequestParam("userId") String userId) {
        return Mono.fromCallable(() -> userService.one(userId))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/create")
    @Operation(summary = "创建用户", tags = {"user"}, description = "请使用此接口创建用户")
    @Parameters({
            @Parameter(name = "userDto", description = "用户信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<UserVo> create(@RequestBody UserDto userDto) {
        return Mono.fromCallable(() -> userService.create(userDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/update")
    @Operation(summary = "更新用户", tags = {"user"}, description = "请使用此接口更新用户")
    @Parameters({
            @Parameter(name = "userDto", description = "用户信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> update(@RequestBody UserDto userDto) {
        return Mono.fromCallable(() -> userService.update(userDto))
                .subscribeOn(getScheduler());
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户", tags = {"user"}, description = "请使用此接口删除用户")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<Boolean> delete(@RequestParam("userId") String userId) {
        return Mono.fromCallable(() -> userService.delete(userId))
                .subscribeOn(getScheduler());
    }
}
