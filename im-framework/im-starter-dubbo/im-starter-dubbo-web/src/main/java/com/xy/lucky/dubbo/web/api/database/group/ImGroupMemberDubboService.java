package com.xy.lucky.dubbo.web.api.database.group;

import com.xy.lucky.domain.po.ImGroupMemberPo;

import java.util.List;

/**
 * 群成员Dubbo服务接口
 */
public interface ImGroupMemberDubboService {

    /**
     * 获取群成员列表
     *
     * @param groupId 群id
     * @return 群成员集合
     */
    List<ImGroupMemberPo> queryList(String groupId);

    /**
     * 获取单个群成员
     *
     * @param groupId  群id
     * @param memberId 成员id
     * @return 单个群成员
     */
    ImGroupMemberPo queryOne(String groupId, String memberId);

    /**
     * 按角色查询群成员
     *
     * @param groupId 群id
     * @param role    角色
     * @return 群成员集合
     */
    List<ImGroupMemberPo> queryByRole(String groupId, Integer role);

    /**
     * 群成员退出群聊
     *
     * @param memberId 成员id
     * @return 是否成功
     */
    Boolean removeOne(String memberId);

    /**
     * 删除群所有成员
     *
     * @param groupId 群id
     * @return 是否成功
     */
    Boolean removeByGroupId(String groupId);

    /**
     * 添加群成员
     *
     * @param groupMember 群成员信息
     * @return 是否成功
     */
    Boolean creat(ImGroupMemberPo groupMember);

    /**
     * 修改群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否成功
     */
    Boolean modify(ImGroupMemberPo groupMember);

    /**
     * 批量修改群成员信息
     *
     * @param groupMemberList 群成员信息列表
     * @return 是否成功
     */
    Boolean modifyBatch(List<ImGroupMemberPo> groupMemberList);

    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     * @return 是否成功
     */
    Boolean creatBatch(List<ImGroupMemberPo> groupMemberList);

    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId 群ID
     * @return 用户头像列表
     */
    List<String> queryNinePeopleAvatar(String groupId);

    /**
     * 统计群成员数量
     *
     * @param groupId 群id
     * @return 成员数量
     */
    Long countByGroupId(String groupId);
}
