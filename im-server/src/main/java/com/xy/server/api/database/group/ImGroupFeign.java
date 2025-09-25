package com.xy.server.api.database.group;


import com.xy.domain.po.ImGroupInviteRequestPo;
import com.xy.domain.po.ImGroupMemberPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.server.api.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "group", value = "im-database", path = "/api/v1/database/group", configuration = FeignRequestInterceptor.class)
public interface ImGroupFeign {


    /**
     * 获取单个群成员
     *
     * @param groupId  群id
     * @param memberId 成员id
     * @return 单个群成员
     */
    @GetMapping("/member/getOne")
    ImGroupMemberPo getOneMember(@RequestParam("groupId") String groupId, @RequestParam("memberId") String memberId);

    /**
     * 获取群信息
     *
     * @param groupId 成员id
     * @return
     */
    @GetMapping("/getOne")
    ImGroupPo getOneGroup(@RequestParam("groupId") String groupId);

    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     */
    @PostMapping("/insert")
    Boolean insert(@RequestBody ImGroupPo groupPo);


    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     */
    @PutMapping("/updateById")
    Boolean updateById(@RequestBody ImGroupPo groupPo);


    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId
     * @return
     */
    @GetMapping("/member/getNinePeopleAvatar")
    List<String> getNinePeopleAvatar(@RequestParam("groupId") String groupId);


    /**
     * 群成员退出群聊
     *
     * @param memberId 成员id
     */
    @DeleteMapping("/member/{memberId}")
    Boolean deleteById(@PathVariable("memberId") String memberId);


    /**
     * 获取群成员
     *
     * @param groupId 群id
     * @return 群成员集合
     */
    @GetMapping("/member/list")
    List<ImGroupMemberPo> getGroupMemberList(@RequestParam("groupId") String groupId);


    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     */
    @PostMapping("/member/batch/insert")
    Boolean groupMessageMemberBatchInsert(@RequestBody List<ImGroupMemberPo> groupMemberList);



    /**
     * 群成员申请加入群聊
     *
     * @param imGroupInviteRequestPo 群成员申请信息
     */
    @PostMapping("/invite/saveOrUpdate")
    Boolean groupInviteSaveOrUpdate(@RequestBody ImGroupInviteRequestPo imGroupInviteRequestPo);


    /**
     * 群成员申请加入群聊 插入
     *
     * @param imGroupInviteRequestPo 群成员申请信息
     */
    @PostMapping("/invite/saveOrUpdate/batch")
    Boolean groupInviteSaveOrUpdateBatch(@RequestBody List<ImGroupInviteRequestPo> imGroupInviteRequestPo);

    /**
     * 获取群成员申请信息
     *
     * @param requestId 申请id
     * @return 群成员申请信息
     */
    @GetMapping("/invite/getOne")
    ImGroupInviteRequestPo getOneInviteById(@RequestParam("requestId") String requestId);


}
