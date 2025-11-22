package com.xy.lucky.database.controller;

import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImGroupMemberService;
import com.xy.lucky.domain.po.ImGroupMemberPo;
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
    public List<ImGroupMemberPo> selectList(@RequestParam("groupId") String groupId) {
        return imGroupMemberService.selectList(groupId);
    }

    /**
     * 获取群成员信息
     *
     * @param groupId  群id
     * @param memberId 成员id
     * @return 群成员信息
     */
    @GetMapping("/selectOne")
    public ImGroupMemberPo selectOne(@RequestParam("groupId") String groupId, @RequestParam("memberId") String memberId) {
        return imGroupMemberService.selectOne(groupId, memberId);
    }

    /**
     * 插入群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImGroupMemberPo groupMember) {
        return imGroupMemberService.insert(groupMember);
    }

    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     */
    @PostMapping("/batchInsert")
    public Boolean batchInsert(@RequestBody List<ImGroupMemberPo> groupMemberList) {
        return imGroupMemberService.batchInsert(groupMemberList);
    }

    /**
     * 更新群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImGroupMemberPo groupMember) {
        return imGroupMemberService.update(groupMember);
    }

    /**
     * 删除群成员信息
     *
     * @param memberId 群成员id
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("memberId") String memberId) {
        return imGroupMemberService.deleteById(memberId);
    }

    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId
     * @return
     */
    @GetMapping("/selectNinePeopleAvatar")
    public List<String> selectNinePeopleAvatar(@RequestParam("groupId") String groupId) {
        return imGroupMemberService.selectNinePeopleAvatar(groupId);
    }

}