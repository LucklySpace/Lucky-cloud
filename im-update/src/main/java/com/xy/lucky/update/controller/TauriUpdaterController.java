package com.xy.lucky.update.controller;

import com.xy.lucky.update.domain.tauri.TauriUpdaterResponse;
import com.xy.lucky.update.service.TauriUpdaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 更新检查与文件下载接口（带 OpenAPI 注解，Knife4j 可直接识别）
 */
@Tag(name = "Tauri Updater", description = "应用更新检查与安装包下载接口")
@RestController
@RequestMapping("/api/{version}/tauri/update")
public class TauriUpdaterController {

    @Autowired
    private TauriUpdaterService tauriUpdaterService;

    /**
     * 获取最新版本信息
     * <p>
     * - 通过请求头传入 platform（可选），返回对应平台信息（若存在）
     * - 返回 DTO: TauriUpdaterResponse
     */
    @Operation(summary = "获取最新版本信息", description = "返回最新版本号、更新说明、发布时间以及平台相关信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回版本信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TauriUpdaterResponse.class)
                    )
            )
    })
    @GetMapping("/latest")
    public TauriUpdaterResponse latest(
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true)
            @PathVariable("version") String apiVersion,

            @Parameter(in = ParameterIn.HEADER, name = "platform", description = "目标平台 (例如: windows-x86_64, mac-aarch64)", required = false)
            @RequestHeader(value = "platform", required = false, defaultValue = "windows-x86_64") String platform
    ) {
        // 直接委托 Service 层构造响应
        return tauriUpdaterService.latest(platform);
    }

    /**
     * 下载指定文件（流式返回）
     * <p>
     * - fileName 支持带扩展名（例如 app-1.2.3.msi）
     * - 返回 Content-Type 为二进制流，Swagger 中标记为 binary
     */
    @Operation(summary = "下载安装包", description = "按文件名下载安装包，Content-Type 为 application/octet-stream")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "返回文件（二进制）",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "文件不存在或不可读"),
            @ApiResponse(responseCode = "403", description = "访问被拒绝（路径越权等）"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true)
            @PathVariable("version") String apiVersion,

            @Parameter(in = ParameterIn.PATH, name = "fileName", description = "要下载的文件名（含扩展名），支持中文，注意不能包含路径穿越）", required = true)
            @PathVariable("fileName") String fileName
    ) {
        // 委托 Service 返回 ResponseEntity（Service 内做路径校验与 Resource 加载）
        return tauriUpdaterService.downloadFile(fileName);
    }
}
