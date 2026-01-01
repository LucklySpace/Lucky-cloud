package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImFriendshipGroupMapper;
import com.xy.lucky.domain.po.ImFriendshipGroupPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImFriendshipGroupService extends ServiceImpl<ImFriendshipGroupMapper, ImFriendshipGroupPo> {

    private final ImFriendshipGroupMapper imFriendshipGroupMapper;

    public List<ImFriendshipGroupPo> queryList() {
        return super.list();
    }

    public ImFriendshipGroupPo queryOne(String id) {
        return imFriendshipGroupMapper.selectById(id);
    }

    public boolean creat(ImFriendshipGroupPo friendshipGroupPo) {
        return super.save(friendshipGroupPo);
    }

    public boolean creatBatch(List<ImFriendshipGroupPo> friendshipGroupPoList) {
        return super.saveBatch(friendshipGroupPoList);
    }

    public boolean modify(ImFriendshipGroupPo friendshipGroupPo) {
        return super.updateById(friendshipGroupPo);
    }

    public boolean removeOne(String id) {
        return super.removeById(id);
    }
}
