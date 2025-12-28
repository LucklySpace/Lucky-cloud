package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.AreaVo;
import com.xy.lucky.platform.exception.ResponseNotIntercept;
import com.xy.lucky.platform.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 地址与 IP 查询接口
 * <p>
 * 提供：
 * - IP 查询对应区域
 * - 根据编号与路径查询区域
 * - 格式化区域路径
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/address")
@RequiredArgsConstructor
@Tag(name = "address", description = "IP 与区域查询接口")
public class AddressController {

    private final AddressService addressService;

    /**
     * 通过 IP 查询对应区域
     */
    @Operation(summary = "IP 查询区域", description = "根据 IPv4 地址查询地区节点")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AreaVo.class)))
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "ip", description = "IPv4 地址，如 8.8.8.8", required = true)
    })
    @GetMapping("/ip/{ip}")
    public Mono<AreaVo> queryByIp(@NotBlank(message = "IP 不能为空") @PathVariable("ip") String ip) {
        log.info("收到 IP 区域查询请求 ip={}", ip);
        return Mono.fromCallable(() -> addressService.getAreaByIp(ip).setIp(ip))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 通过地区编号查询节点
     */
    @Operation(summary = "编号查询区域", description = "根据地区编号返回地区节点")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AreaVo.class)))
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "id", description = "地区编号", required = true)
    })
    @GetMapping("/area/{id}")
    public Mono<AreaVo> queryById(@Min(value = 0, message = "id 必须为非负数") @PathVariable("id") Integer id) {
        log.info("收到编号区域查询请求 id={}", id);
        return Mono.fromCallable(() -> addressService.getAreaById(id))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 解析区域路径（如：河南省/郑州市/金水区）
     */
    @Operation(summary = "路径解析区域", description = "输入完整路径，返回地区节点")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "解析成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AreaVo.class)))
    })
    @Parameters({
            @Parameter(in = ParameterIn.QUERY, name = "path", description = "完整路径，如 上海上海市静安区", required = true)
    })
    @GetMapping("/area/parse")
    public Mono<AreaVo> parse(@NotBlank(message = "path 不能为空") @RequestParam("path") String path) {
        log.info("收到路径区域解析请求 path={}", path);
        return Mono.fromCallable(() -> addressService.parseArea(path))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 将地区编号格式化为人类可读路径
     */
    @Operation(summary = "格式化区域路径", description = "根据编号返回格式化路径，以 ' / ' 分隔")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "格式化成功",
                    content = @Content(mediaType = "text/plain",
                            schema = @Schema(type = "string")))
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true),
            @Parameter(in = ParameterIn.PATH, name = "id", description = "地区编号", required = true)
    })
    @ResponseNotIntercept
    @GetMapping("/area/format/{id}")
    public Mono<String> format(@Min(value = 0, message = "id 必须为非负数") @PathVariable("id") Integer id) {
        log.info("收到地区路径格式化请求 id={}", id);
        return Mono.fromCallable(() -> addressService.formatArea(id))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
