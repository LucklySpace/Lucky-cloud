package com.xy.generator.controller;

import com.xy.core.model.IMetaId;
import com.xy.generator.service.IdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ID生成接口控制器
 * 提供多种ID生成策略的REST API接口
 */
@Tag(name = "ID Generator", description = "ID生成服务接口")
@RestController
@RequestMapping("/api/v1/generator")
public class IdController {

    @Resource
    private IdService idService;

    /**
     * 根据类型和业务标识异步生成单个ID
     *
     * @param type 策略类型：snowflake | redis | uid | uuid
     * @param key  业务标识
     * @return ID对象
     */
    @Operation(summary = "生成单个ID", description = "根据指定策略和业务标识生成单个ID")
    @GetMapping("/id")
    public IMetaId generateId(
            @Parameter(description = "策略类型") @RequestParam("type") String type,
            @Parameter(description = "业务标识") @RequestParam("key") String key) {
        return idService.generateId(type, key);
    }

    /**
     * 批量获取ID
     *
     * @param type  策略类型：snowflake | redis | uid | uuid
     * @param key   业务标识
     * @param count 获取数量
     * @return ID列表
     */
    @Operation(summary = "批量生成ID", description = "根据指定策略和业务标识批量生成ID")
    @GetMapping("/ids")
    public List<IMetaId> generateBatchIds(
            @Parameter(description = "策略类型") @RequestParam("type") String type,
            @Parameter(description = "业务标识") @RequestParam("key") String key,
            @Parameter(description = "生成数量") @RequestParam("count") Integer count) {
        return idService.generateIds(type, key, count);
    }
}