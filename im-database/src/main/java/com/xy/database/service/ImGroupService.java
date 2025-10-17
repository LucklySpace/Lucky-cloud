package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImGroupPo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImGroupService extends IService<ImGroupPo> {

    /**
     * 插入群信息
     * @param groupPo 群信息
     * @return 是否成功
     */
    boolean insert(ImGroupPo groupPo);

    /**
     * 批量插入群信息
     * @param groupPoList 群信息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImGroupPo> groupPoList);

    /**
     * 查询单条群信息
     * @param groupId 群ID
     * @return 群信息
     */
    ImGroupPo selectOne(String groupId);
    
    /**
     * 根据ID查询群信息
     * @param groupId 群ID
     * @return 群信息
     */
    ImGroupPo selectById(String groupId);
    
    /**
     * 统计群数量
     * @return 群数量
     */
    long count();
    
    /**
     * 查询群列表
     * @return 群列表
     */
    List<ImGroupPo> selectList();
    
    /**
     * 更新群信息
     * @param groupPo 群信息
     * @return 是否成功
     */
    boolean update(ImGroupPo groupPo);
    
    /**
     * 根据ID删除群
     * @param groupId 群ID
     * @return 是否成功
     */
    boolean deleteById(String groupId);

    List<ImGroupPo> list(String userId);

    // 查询九人
    List<String> selectNinePeople(String groupId);
}