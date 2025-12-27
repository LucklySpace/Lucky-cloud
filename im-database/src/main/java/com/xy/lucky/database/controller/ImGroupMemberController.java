package com.xy.lucky.database.controller;

import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImGroupMemberService;
import com.xy.lucky.domain.po.ImGroupMemberPo;
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
@RequestMapping("/api/{version}/database/group/member")
@Tag(name = "ImGroupMember", description = "群成员数据库接口")
public class ImGroupMemberController {

    @Resource
    private ImGroupMemberService imGroupMemberService;

    /**
     * 查询群成员
     *
     * @param groupId 群id
     * @return 群成员信息
     */
    @GetMapping("/selectList")
    @Operation(summary = "查询群成员列表", description = "根据群ID查询群成员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMemberPo.class)))
    })
    public List<ImGroupMemberPo> listGroupMembers(@RequestParam("groupId") String groupId) {
        return imGroupMemberService.queryList(groupId);
    }

    /**
     * 获取群成员信息
     *
     * @param groupId  群id
     * @param memberId 成员id
     * @return 群成员信息
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据群ID与成员ID获取群成员信息", description = "返回单个群成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMemberPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImGroupMemberPo getGroupMember(@RequestParam("groupId") String groupId, @RequestParam("memberId") String memberId) {
        return imGroupMemberService.queryOne(groupId, memberId);
    }

    /**
     * 插入群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    @Operation(summary = "添加群成员", description = "新增群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createGroupMember(@RequestBody ImGroupMemberPo groupMember) {
        return imGroupMemberService.creat(groupMember);
    }

    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量添加群成员", description = "批量新增群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createGroupMembersBatch(@RequestBody List<ImGroupMemberPo> groupMemberList) {
        return imGroupMemberService.creatBatch(groupMemberList);
    }

    /**
     * 更新群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    @Operation(summary = "更新群成员信息", description = "根据ID更新群成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateGroupMember(@RequestBody ImGroupMemberPo groupMember) {
        return imGroupMemberService.modify(groupMember);
    }

    /**
     * 删除群成员信息
     *
     * @param memberId 群成员id
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群成员", description = "根据ID删除群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteGroupMemberById(@RequestParam("memberId") String memberId) {
        return imGroupMemberService.removeOne(memberId);
    }

    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId
     * @return
     */
    @GetMapping("/selectNinePeopleAvatar")
    @Operation(summary = "随机获取九宫格头像", description = "随机返回9个成员头像")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public List<String> listNineAvatars(@RequestParam("groupId") String groupId) {
        return imGroupMemberService.queryNinePeopleAvatar(groupId);
    }

}
