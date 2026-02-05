package com.xy.lucky.database.web.controller;


import com.xy.lucky.database.web.security.SecurityInner;
import com.xy.lucky.database.web.service.ImUserDataService;
import com.xy.lucky.domain.po.ImUserDataPo;
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
@RequestMapping("/api/{version}/database/user/data")
@Tag(name = "ImUserData", description = "用户数据数据库接口")
@Validated
public class ImUserDataController {

    @Resource
    private ImUserDataService imUserDataService;

    /**
     * 模糊查询用户信息
     *
     * @param keyword 查询关键字，可以是userId或mobile的部分内容
     * @return 符合条件的用户信息列表
     */
    @GetMapping("/selectList")
    @Operation(summary = "搜索用户数据", description = "按关键词搜索用户数据（userId、mobile等）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserDataPo.class)))
    })
    public Mono<List<ImUserDataPo>> searchUserData(@RequestParam("keyword") @NotBlank @Size(min = 2, max = 64) String keyword) {
        return Mono.fromCallable(() -> imUserDataService.queryByKeyword(keyword)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息集合
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据用户ID获取用户数据", description = "返回指定用户ID的用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserDataPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImUserDataPo> getUserDataById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> imUserDataService.queryOne(userId)).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/selectListByIds")
    @Operation(summary = "根据ID列表批量获取用户数据", description = "通过用户ID集合批量查询用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserDataPo.class)))
    })
    public Mono<List<ImUserDataPo>> listUserDataByIds(@RequestBody @NotEmpty List<@NotBlank @Size(max = 64) String> userIdList) {
        return Mono.fromCallable(() -> imUserDataService.queryListByIds(userIdList)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新用户信息
     *
     * @param po 用户信息
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户数据", description = "根据ID更新用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateUserData(@RequestBody @Valid ImUserDataPo po) {
        return Mono.fromCallable(() -> imUserDataService.updateById(po)).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 插入用户数据信息
     *
     * @param userDataPo 用户数据信息
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    @Operation(summary = "创建用户数据", description = "新增用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createUserData(@RequestBody @Valid ImUserDataPo userDataPo) {
        return Mono.fromCallable(() -> imUserDataService.creat(userDataPo)).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 批量插入用户数据信息
     *
     * @param userDataPoList 用户数据信息列表
     * @return 是否插入成功
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建用户数据", description = "批量新增用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createUserDataBatch(@RequestBody @NotEmpty List<@Valid ImUserDataPo> userDataPoList) {
        return Mono.fromCallable(() -> imUserDataService.creatBatch(userDataPoList)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除用户数据信息
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除用户数据", description = "根据ID删除用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteUserDataById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> imUserDataService.removeOne(userId)).subscribeOn(Schedulers.boundedElastic());
    }
}
