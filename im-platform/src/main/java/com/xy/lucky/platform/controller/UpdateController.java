package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.UpdaterResponseVo;
import com.xy.lucky.platform.service.UpdaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Tauri 应用更新控制器
 * 提供版本检查与安装包下载接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/update")
@RequiredArgsConstructor
@Tag(name = "update", description = "应用更新检查与安装包下载接口")
public class UpdateController {

    private final UpdaterService updaterService;

    /**
     * 获取最新版本信息
     *
     * @return 最新版本信息响应
     */
    @Operation(summary = "获取最新版本信息", description = "返回最新版本号、更新说明、发布时间以及平台相关信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回版本信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdaterResponseVo.class)
                    )
            )
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true)
    })
    @GetMapping("/tauri/latest")
    public Mono<UpdaterResponseVo> latest() {
        log.info("收到获取最新版本信息请求");
        return Mono.fromCallable(updaterService::latest)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 下载指定文件（流式返回）
     *
     * @param fileName 要下载的文件名
     * @return 文件资源响应
     */
    @Operation(summary = "下载安装包", description = "按文件名下载安装包，Content-Type 为 application/octet-stream")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "返回文件（二进制）",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "文件不存在或不可读"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true),
            @Parameter(in = ParameterIn.PATH, name = "fileName", description = "要下载的文件名（含扩展名），支持中文，注意不能包含路径穿越）", required = true)
    })
    @GetMapping("/download/{fileName:.+}")
    public Mono<ResponseEntity<Resource>> downloadFile(@NotBlank(message = "请输入文件名") @PathVariable("fileName") String fileName) {
        log.info("收到文件下载请求，文件名: {}", fileName);
        return Mono.fromCallable(() -> updaterService.downloadFile(fileName))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
