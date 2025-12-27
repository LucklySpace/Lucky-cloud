package com.xy.lucky.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImFriendshipGroupMemberService;
import com.xy.lucky.domain.po.ImFriendshipGroupMemberPo;
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
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend/group/member")
@Tag(name = "ImFriendshipGroupMember", description = "好友分组成员数据库接口")
public class ImFriendshipGroupMemberController {

    @Resource
    private ImFriendshipGroupMemberService imFriendshipGroupMemberService;

    @GetMapping("/selectList")
    @Operation(summary = "查询好友分组成员列表", description = "根据分组ID查询成员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipGroupMemberPo.class)))
    })
    public List<ImFriendshipGroupMemberPo> listFriendshipGroupMembers(@RequestParam("groupId") String groupId) {
        QueryWrapper<ImFriendshipGroupMemberPo> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        return imFriendshipGroupMemberService.list(query);
    }

    @GetMapping("/selectOne")
    @Operation(summary = "获取好友分组成员", description = "根据分组ID与成员ID获取成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipGroupMemberPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImFriendshipGroupMemberPo getFriendshipGroupMember(@RequestParam("groupId") String groupId,
                                                              @RequestParam("memberId") String memberId) {
        QueryWrapper<ImFriendshipGroupMemberPo> query = new QueryWrapper<>();
        query.eq("group_id", groupId).eq("member_id", memberId);
        return imFriendshipGroupMemberService.getOne(query);
    }

    @PostMapping("/insert")
    @Operation(summary = "添加好友分组成员", description = "新增分组成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createFriendshipGroupMember(@RequestBody ImFriendshipGroupMemberPo memberPo) {
        return imFriendshipGroupMemberService.save(memberPo);
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量添加好友分组成员", description = "批量新增分组成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createFriendshipGroupMembersBatch(@RequestBody List<ImFriendshipGroupMemberPo> memberPoList) {
        return imFriendshipGroupMemberService.saveBatch(memberPoList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友分组成员", description = "根据ID更新分组成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateFriendshipGroupMember(@RequestBody ImFriendshipGroupMemberPo memberPo) {
        return imFriendshipGroupMemberService.updateById(memberPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除好友分组成员", description = "根据成员ID删除分组成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteFriendshipGroupMemberById(@RequestParam("memberId") String memberId) {
        return imFriendshipGroupMemberService.removeById(memberId);
    }
}
