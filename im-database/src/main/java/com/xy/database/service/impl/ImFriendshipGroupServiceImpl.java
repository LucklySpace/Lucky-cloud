package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImFriendshipGroupMapper;
import com.xy.database.service.ImFriendshipGroupService;
import com.xy.domain.po.ImFriendshipGroupPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_friendship_group】的数据库操作Service实现
 */
@Service
public class ImFriendshipGroupServiceImpl extends ServiceImpl<ImFriendshipGroupMapper, ImFriendshipGroupPo>
        implements ImFriendshipGroupService {

    @Override
    public boolean insert(ImFriendshipGroupPo friendshipGroupPo) {
        return this.save(friendshipGroupPo);
    }
    
    @Override
    public boolean batchInsert(List<ImFriendshipGroupPo> friendshipGroupPoList) {
        return this.saveBatch(friendshipGroupPoList);
    }
    
    @Override
    public ImFriendshipGroupPo selectOne(String id) {
        return this.getById(id);
    }
    
    @Override
    public ImFriendshipGroupPo selectById(String id) {
        return this.getById(id);
    }

    @Override
    public List<ImFriendshipGroupPo> selectList() {
        return this.list();
    }
    
    @Override
    public boolean update(ImFriendshipGroupPo friendshipGroupPo) {
        return this.updateById(friendshipGroupPo);
    }
    
    @Override
    public boolean deleteById(String id) {
        return this.removeById(id);
    }
}