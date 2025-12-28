package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImUserDataReactiveService;
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
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database/user/data")
@Tag(name = "ImUserData", description = "用户数据数据库接口(WebFlux-R2DBC)")
@Validated
public class ImUserDataReactiveController {

    @Resource
    private ImUserDataReactiveService imUserDataService;

    @GetMapping("/selectList")
    @Operation(summary = "搜索用户数据", description = "按关键词搜索用户数据（userId、mobile等）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserDataPo.class)))
    })
    public Mono<List<ImUserDataPo>> searchUserData(@RequestParam("keyword") @NotBlank @Size(min = 2, max = 64) String keyword) {
        return imUserDataService.queryByKeyword(keyword).collectList();
    }

    @GetMapping("/selectOne")
    @Operation(summary = "根据用户ID获取用户数据", description = "返回指定用户ID的用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserDataPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImUserDataPo> getUserDataById(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return imUserDataService.queryOne(userId);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户数据", description = "根据ID更新用户数据信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateUserData(@RequestBody @Valid ImUserDataPo po) {
        return imUserDataService.modify(po);
    }
}
