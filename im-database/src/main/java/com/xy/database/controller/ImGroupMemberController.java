package com.xy.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImGroupMemberService;
import com.xy.domain.po.ImGroupMemberPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/group/member")
@Tag(name = "ImGroupMember", description = "群成员数据库接口")
@RequiredArgsConstructor
public class ImGroupMemberController {

    private final ImGroupMemberService imGroupMemberService;


    /**
     * 查询群成员
     *
     * @param groupId 群id
     * @return 群成员信息
     */
    @GetMapping("/list")
    public List<ImGroupMemberPo> getGroupMemberList(@RequestParam("groupId") String groupId) {
        QueryWrapper<ImGroupMemberPo> query = new QueryWrapper<ImGroupMemberPo>()
                .eq("group_id", groupId);
        return imGroupMemberService.list(query);
    }


    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     */
    @PostMapping("/batch/insert")
    public Boolean groupMessageMemberBatchInsert(@RequestBody List<ImGroupMemberPo> groupMemberList) {
        return imGroupMemberService.saveBatch(groupMemberList);
    }

    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId
     * @return
     */
    @GetMapping("/getNinePeopleAvatar")
    public List<String> getNinePeopleAvatar(@RequestParam("groupId") String groupId) {
        return imGroupMemberService.getNinePeopleAvatar(groupId);
    }


}
