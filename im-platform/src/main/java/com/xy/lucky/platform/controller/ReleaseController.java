package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.AssetVo;
import com.xy.lucky.platform.domain.vo.CreateAssetVo;
import com.xy.lucky.platform.domain.vo.CreateReleaseVo;
import com.xy.lucky.platform.domain.vo.ReleaseVo;
import com.xy.lucky.platform.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 应用更新控制器
 * 提供版本检查与安装包下载接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/release")
@RequiredArgsConstructor
@Tag(name = "release", description = "应用发布与安装包资产上传")
public class ReleaseController {

    private final ReleaseService releaseService;

    /**
     * 发布新版本
     *
     * @param createReleaseVo 创建版本信息
     * @return 发布信息
     */
    @Operation(summary = "发布新版本", description = "创建或更新版本发布信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功发布版本",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReleaseVo.class)
                    )
            )
    })
    @PostMapping("/release")
    public ReleaseVo publishRelease(@Valid @RequestBody CreateReleaseVo createReleaseVo) {
        log.info("收到发布版本请求，版本号: {}", createReleaseVo.getVersion());
        return releaseService.publishRelease(createReleaseVo);
    }

    /**
     * 发布版本并上传安装包
     *
     * @param createAssetVo 资产信息
     * @param file          上传的文件
     * @return 资产信息
     */
    @Operation(summary = "发布版本并上传安装包", description = "创建发布并上传安装包到 MinIO，返回发布与资产信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功发布版本并上传文件",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AssetVo.class)
                    )
            )
    })
    @PostMapping(value = "/releases/assets")
    public AssetVo publishAsset(
            @Parameter(description = "资产信息", required = true) @Valid @RequestPart("createAssetVo") CreateAssetVo createAssetVo,
            @Parameter(description = "上传文件", required = true) @RequestPart("file") MultipartFile file
    ) {
        log.info("收到发布资产请求，版本号: {}，平台: {}", createAssetVo.getVersion(), createAssetVo.getPlatform());
        return releaseService.publishAsset(createAssetVo, file);
    }
}