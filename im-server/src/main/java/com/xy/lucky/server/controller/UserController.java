package com.xy.lucky.server.controller;


import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户
 */
@Slf4j
@RestController
@RequestMapping("/api/{version}/user")
@Tag(name = "user", description = "用户")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/list")
    @Operation(summary = "查询用户列表", tags = {"user"}, description = "请使用此接口查找用户列表")
    @Parameters({
            @Parameter(name = "userDto", description = "用户信息", required = true, in = ParameterIn.DEFAULT)
    })
    public List<UserVo> list(@RequestBody UserDto userDto) {
        return userService.list(userDto);
    }

    @GetMapping("/one")
    @Operation(summary = "查询用户信息", tags = {"user"}, description = "请使用此接口获取用户信息")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public UserVo one(@RequestParam("userId") String userId) {
        return userService.one(userId);
    }

    @PostMapping("/create")
    @Operation(summary = "创建用户", tags = {"user"}, description = "请使用此接口创建用户")
    @Parameters({
            @Parameter(name = "userDto", description = "用户信息", required = true, in = ParameterIn.DEFAULT)
    })
    public UserVo create(@RequestBody UserDto userDto) {
        return userService.create(userDto);
    }

    @PostMapping("/update")
    @Operation(summary = "更新用户", tags = {"user"}, description = "请使用此接口更新用户")
    @Parameters({
            @Parameter(name = "userDto", description = "用户信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Result update(@RequestBody UserDto userDto) {
        return userService.update(userDto);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户", tags = {"user"}, description = "请使用此接口删除用户")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Result delete(@RequestParam("userId") String userId) {
        return userService.delete(userId);
    }
}