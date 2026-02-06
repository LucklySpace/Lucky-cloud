package com.xy.lucky.lbs.controller;

import com.xy.lucky.lbs.domain.dto.LocationUpdateDto;
import com.xy.lucky.lbs.domain.dto.NearbySearchDto;
import com.xy.lucky.lbs.domain.vo.LocationVo;
import com.xy.lucky.lbs.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 位置服务控制器
 * 提供位置上报和附近的人搜索接口
 */
@Slf4j
@RestController
@RequestMapping({"/api/lbs", "/api/{version}/lbs"})
@RequiredArgsConstructor
@Tag(name = "location", description = "位置服务接口")
public class LocationController {

    private final LocationService locationService;

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    private String getCurrentUserId() {
        try {
            Object principal = "";
            if (principal instanceof String) {
                return (String) principal;
            }
            // 兼容其他类型的 Principal，视具体 Auth 实现而定
            return principal.toString();
        } catch (Exception e) {
            log.warn("从上下文获取当前用户失败", e);
        }
        // 这里抛出异常会被全局异常处理器捕获，建议使用自定义业务异常
        throw new RuntimeException("用户未认证或上下文为空");
    }

    /**
     * 上报用户位置
     *
     * @param dto 位置信息DTO
     * @return 操作结果
     */
    @PostMapping("/location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @Operation(summary = "上报用户位置")
    public void reportLocation(@RequestBody LocationUpdateDto dto) {
        String userId = getCurrentUserId();
        locationService.updateLocation(userId, dto);
    }

    /**
     * 搜索附近用户
     *
     * @param dto 搜索条件DTO
     * @return 附近用户列表
     */
    @PostMapping("/nearby")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LocationVo.class)))
    })
    @Operation(summary = "搜索附近用户")
    public List<LocationVo> searchNearby(@RequestBody NearbySearchDto dto) {
        String userId = getCurrentUserId();
        return locationService.searchNearby(userId, dto);
    }
}
