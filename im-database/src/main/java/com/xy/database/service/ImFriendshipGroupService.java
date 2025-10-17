package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImFriendshipGroupPo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_friendship_group】的数据库操作Service
 */
public interface ImFriendshipGroupService extends IService<ImFriendshipGroupPo> {
    
    /**
     * 插入好友分组信息
     * @param friendshipGroupPo 好友分组信息
     * @return 是否成功
     */
    boolean insert(ImFriendshipGroupPo friendshipGroupPo);

    /**
     * 批量插入好友分组信息
     * @param friendshipGroupPoList 好友分组信息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImFriendshipGroupPo> friendshipGroupPoList);

    /**
     * 查询单条好友分组信息
     * @param id 好友分组ID
     * @return 好友分组信息
     */
    ImFriendshipGroupPo selectOne(String id);
    
    /**
     * 根据ID查询好友分组信息
     * @param id 好友分组ID
     * @return 好友分组信息
     */
    ImFriendshipGroupPo selectById(String id);

    /**
     * 查询好友分组列表
     * @return 好友分组列表
     */
    List<ImFriendshipGroupPo> selectList();
    
    /**
     * 更新好友分组信息
     * @param friendshipGroupPo 好友分组信息
     * @return 是否成功
     */
    boolean update(ImFriendshipGroupPo friendshipGroupPo);
    
    /**
     * 根据ID删除好友分组
     * @param id 好友分组ID
     * @return 是否成功
     */
    boolean deleteById(String id);
}