package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImUserDataPo;

import java.util.List;

public interface ImUserDataService extends IService<ImUserDataPo> {
    
    /**
     * 插入用户数据信息
     * @param userDataPo 用户数据信息
     * @return 是否成功
     */
    boolean insert(ImUserDataPo userDataPo);

    /**
     * 批量插入用户数据信息
     * @param userDataPoList 用户数据信息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImUserDataPo> userDataPoList);

    /**
     * 查询单条用户数据信息
     * @param id 用户数据ID
     * @return 用户数据信息
     */
    ImUserDataPo selectOne(String id);
    
    /**
     * 根据ID查询用户数据信息
     * @param id 用户数据ID
     * @return 用户数据信息
     */
    ImUserDataPo selectById(String id);
    
    /**
     * 查询用户数据列表
     * @return 用户数据列表
     */
    List<ImUserDataPo> selectList();
    
    /**
     * 更新用户数据信息
     * @param userDataPo 用户数据信息
     * @return 是否成功
     */
    boolean update(ImUserDataPo userDataPo);
    
    /**
     * 根据ID删除用户数据
     * @param id 用户数据ID
     * @return 是否成功
     */
    boolean deleteById(String id);
}