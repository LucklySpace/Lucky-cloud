package com.xy.lucky.database.controller;

import com.xy.lucky.database.service.ImGroupService;
import com.xy.lucky.domain.po.ImGroupPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/group")
@Tag(name = "ImGroup", description = "群数据库接口")
public class ImGroupController {

    @Resource
    private ImGroupService imGroupService;

    /**
     * 查询群列表
     */
    @GetMapping("/selectList")
    @Operation(summary = "查询用户所在群列表", description = "根据用户ID查询其加入的群")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupPo.class)))
    })
    public List<ImGroupPo> listGroupsByUser(@RequestParam("userId") String userId) {
        return imGroupService.queryList(userId);
    }

    /**
     * 获取群信息
     *
     * @param groupId 群id
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据群ID获取群信息", description = "返回群基础信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImGroupPo getGroupById(@RequestParam("groupId") String groupId) {
        return imGroupService.queryOne(groupId);
    }

    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     */
    @PostMapping("/insert")
    @Operation(summary = "创建群", description = "新增群信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createGroup(@RequestBody ImGroupPo groupPo) {
        return imGroupService.creat(groupPo);
    }

    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新群信息", description = "根据ID更新群基础信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateGroup(@RequestBody ImGroupPo groupPo) {
        return imGroupService.modify(groupPo);
    }

    /**
     * 删除群
     *
     * @param groupId 群id
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群", description = "根据ID删除群")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteGroupById(@RequestParam("groupId") String groupId) {
        return imGroupService.removeOne(groupId);
    }

}
