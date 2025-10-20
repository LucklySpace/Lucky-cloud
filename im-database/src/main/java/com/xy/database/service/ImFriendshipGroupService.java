package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImFriendshipGroupMapper;
import com.xy.domain.po.ImFriendshipGroupPo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImFriendshipGroupService extends ServiceImpl<ImFriendshipGroupMapper, ImFriendshipGroupPo>
        implements IService<ImFriendshipGroupPo> {

    public List<ImFriendshipGroupPo> selectList() {
        return this.list();
    }

    public ImFriendshipGroupPo selectOne(String id) {
        return this.getById(id);
    }

    public boolean insert(ImFriendshipGroupPo friendshipGroupPo) {
        return this.save(friendshipGroupPo);
    }

    public boolean batchInsert(List<ImFriendshipGroupPo> friendshipGroupPoList) {
        return this.saveBatch(friendshipGroupPoList);
    }

    public boolean update(ImFriendshipGroupPo friendshipGroupPo) {
        return this.updateById(friendshipGroupPo);
    }

    public boolean deleteById(String id) {
        return this.removeById(id);
    }
}