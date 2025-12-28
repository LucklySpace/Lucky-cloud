package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImUserReactiveService;
import com.xy.lucky.domain.po.ImUserPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database/user")
@Tag(name = "ImUser", description = "用户数据库接口(WebFlux-R2DBC)")
@Validated
public class ImUserReactiveController {

    @Resource
    private ImUserReactiveService imUserService;

    @GetMapping("/selectOne")
    @Operation(summary = "根据用户ID获取用户信息", description = "返回指定用户ID的用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserPo.class)))
    })
    public Mono<ImUserPo> getUserById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return imUserService.queryOne(userId);
    }

    @GetMapping("/selectOneByMobile")
    @Operation(summary = "根据手机号获取用户信息", description = "通过手机号查询用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserPo.class)))
    })
    public Mono<ImUserPo> getUserByMobile(@RequestParam("mobile") @NotBlank @Size(min = 5, max = 32) String mobile) {
        return imUserService.queryOneByMobile(mobile);
    }

    @PostMapping("/selectListByIds")
    @Operation(summary = "根据ID列表批量获取用户", description = "通过用户ID集合批量查询用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserPo.class)))
    })
    public Mono<List<ImUserPo>> listUsersByIds(@RequestBody @NotEmpty List<@NotBlank @Size(max = 64) String> userIdList) {
        return imUserService.listByIds(userIdList).collectList();
    }

    @PostMapping("/insert")
    @Operation(summary = "创建用户", description = "新增用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createUser(@RequestBody @Valid ImUserPo userPo) {
        return imUserService.create(userPo);
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建用户", description = "批量新增用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createUsersBatch(@RequestBody @NotEmpty List<@Valid ImUserPo> userPoList) {
        return imUserService.createBatch(userPoList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户", description = "根据ID更新用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateUser(@RequestBody @Valid ImUserPo userPo) {
        return imUserService.modify(userPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除用户", description = "根据ID删除用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteUserById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return imUserService.removeOne(userId);
    }

    @GetMapping("/count")
    public Mono<Long> count() {
        return imUserService.count();
    }
}
