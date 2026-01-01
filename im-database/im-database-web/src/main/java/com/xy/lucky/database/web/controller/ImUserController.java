package com.xy.lucky.database.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.lucky.database.web.security.SecurityInner;
import com.xy.lucky.database.web.service.ImUserService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/user")
@Tag(name = "ImUser", description = "用户数据库接口")
@Validated
public class ImUserController {

    @Resource
    private ImUserService imUserService;

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息集合
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据用户ID获取用户信息", description = "返回指定用户ID的用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserPo.class)))
    })
    public Mono<ImUserPo> getUserById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> imUserService.queryOne(userId)).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 获取用户信息
     *
     * @param mobile 用户手机号
     * @return 用户信息集合
     */
    @GetMapping("/selectOneByMobile")
    @Operation(summary = "根据手机号获取用户信息", description = "通过手机号查询用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserPo.class)))
    })
    public Mono<ImUserPo> getUserByMobile(@RequestParam("mobile") @NotBlank @Size(min = 5, max = 32) String mobile) {
        QueryWrapper<ImUserPo> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile", mobile);
        return Mono.fromCallable(() -> imUserService.getOne(wrapper)).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/selectListByIds")
    @Operation(summary = "根据ID列表批量获取用户", description = "通过用户ID集合批量查询用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserPo.class)))
    })
    public Mono<List<ImUserPo>> listUsersByIds(@RequestBody @NotEmpty List<@NotBlank @Size(max = 64) String> userIdList) {
        return Mono.fromCallable(() -> imUserService.listByIds(userIdList)).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 插入用户信息
     *
     * @param userPo 用户信息
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    @Operation(summary = "创建用户", description = "新增用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createUser(@RequestBody @Valid ImUserPo userPo) {
        return Mono.fromCallable(() -> imUserService.creat(userPo)).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 批量插入用户信息
     *
     * @param userPoList 用户信息列表
     * @return 是否插入成功
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建用户", description = "批量新增用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createUsersBatch(@RequestBody @NotEmpty List<@Valid ImUserPo> userPoList) {
        return Mono.fromCallable(() -> imUserService.creatBatch(userPoList)).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 更新用户信息
     *
     * @param userPo 用户信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    @Operation(summary = "更新用户", description = "根据ID更新用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateUser(@RequestBody @Valid ImUserPo userPo) {
        return Mono.fromCallable(() -> imUserService.modify(userPo)).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 删除用户信息
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除用户", description = "根据ID删除用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteUserById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> imUserService.removeOne(userId)).subscribeOn(Schedulers.boundedElastic());
    }
}
