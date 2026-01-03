package com.xy.lucky.lbs.controller;

import com.xy.lucky.lbs.domain.vo.AddressVo;
import com.xy.lucky.lbs.domain.vo.RegionVo;
import com.xy.lucky.lbs.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/lbs/region", "/api/{version}/lbs/region"})
@Tag(name = "region", description = "中国省市区街道村查询接口")
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/nearest-county")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "根据经纬度获取最近的区县")
    public RegionVo findNearestCounty(@RequestParam("lat") Double lat, @RequestParam("lng") Double lng) {
        return regionService.findNearestCounty(lat, lng);
    }

    @GetMapping("/search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "行政区划模糊搜索(支持省市区乡镇)")
    public List<RegionVo> searchRegions(@RequestParam("keyword") String keyword) {
        return regionService.searchRegions(keyword);
    }

    @GetMapping("/reverse-geo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AddressVo.class)))
    })
    @Operation(summary = "逆地理编码(坐标转完整地址)")
    public AddressVo reverseGeocoding(@RequestParam("lat") Double lat, @RequestParam("lng") Double lng) {
        return regionService.reverseGeocoding(lat, lng);
    }

    @GetMapping("/provinces")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "获取所有省份")
    public List<RegionVo> getProvinces() {
        return regionService.getProvinces();
    }

    @GetMapping("/cities")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "获取省份下的城市")
    public List<RegionVo> getCities(@RequestParam("code") Long provinceCode) {
        return regionService.getCities(provinceCode);
    }

    @GetMapping("/counties")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "获取城市下的区县")
    public List<RegionVo> getCounties(@RequestParam("code") Long cityCode) {
        return regionService.getCounties(cityCode);
    }

    @GetMapping("/towns")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "获取区县下的乡镇")
    public List<RegionVo> getTowns(@RequestParam("code") Long countyCode) {
        return regionService.getTowns(countyCode);
    }

    @GetMapping("/villages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegionVo.class)))
    })
    @Operation(summary = "获取乡镇下的村/社区")
    public List<RegionVo> getVillages(@RequestParam("code") Long townCode) {
        return regionService.getVillages(townCode);
    }
}
