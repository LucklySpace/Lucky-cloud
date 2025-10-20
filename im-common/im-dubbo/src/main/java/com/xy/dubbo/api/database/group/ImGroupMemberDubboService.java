package com.xy.dubbo.api.database.group;

import com.xy.domain.po.ImGroupMemberPo;

import java.util.List;

/**
 * 群成员Dubbo服务接口
 */
public interface ImGroupMemberDubboService {

    /**
     * 获取群成员
     *
     * @param groupId 群id
     * @return 群成员集合
     */
    List<ImGroupMemberPo> selectList(String groupId);

    /**
     * 获取单个群成员
     *
     * @param groupId  群id
     * @param memberId 成员id
     * @return 单个群成员
     */
    ImGroupMemberPo selectOne(String groupId, String memberId);

    /**
     * 群成员退出群聊
     *
     * @param memberId 成员id
     * @return 是否成功
     */
    Boolean deleteById(String memberId);

    /**
     * 添加群成员
     *
     * @param groupMember 群成员信息
     * @return 是否成功
     */
    Boolean insert(ImGroupMemberPo groupMember);

    /**
     * 修改群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否成功
     */
    Boolean update(ImGroupMemberPo groupMember);

    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     * @return 是否成功
     */
    Boolean batchInsert(List<ImGroupMemberPo> groupMemberList);


    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId 群ID
     * @return 用户头像列表
     */
    List<String> selectNinePeopleAvatar(String groupId);
}