package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.api.group.ImGroupMemberDubboService;
import com.xy.lucky.database.web.mapper.ImGroupMemberMapper;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 群成员服务实现
 *
 * @author xy
 */
@DubboService
@RequiredArgsConstructor
public class ImGroupMemberService extends ServiceImpl<ImGroupMemberMapper, ImGroupMemberPo>
        implements ImGroupMemberDubboService {

    private final ImGroupMemberMapper imGroupMemberMapper;

    @Override
    public List<ImGroupMemberPo> queryList(String groupId) {
        QueryWrapper<ImGroupMemberPo> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        return super.list(wrapper);
    }

    @Override
    public ImGroupMemberPo queryOne(String groupId, String memberId) {
        QueryWrapper<ImGroupMemberPo> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("member_id", memberId);
        return super.getOne(wrapper);
    }

    @Override
    public List<ImGroupMemberPo> queryByRole(String groupId, Integer role) {
        QueryWrapper<ImGroupMemberPo> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("role", role);
        return super.list(wrapper);
    }

    @Override
    public List<String> queryNinePeopleAvatar(String groupId) {
        return imGroupMemberMapper.selectNinePeopleAvatar(groupId);
    }

    @Override
    public Boolean creat(ImGroupMemberPo groupMember) {
        return super.save(groupMember);
    }

    @Override
    public Boolean modify(ImGroupMemberPo groupMember) {
        return super.updateById(groupMember);
    }

    @Override
    public Boolean modifyBatch(List<ImGroupMemberPo> groupMemberList) {
        return super.updateBatchById(groupMemberList);
    }

    @Override
    public Boolean creatBatch(List<ImGroupMemberPo> groupMemberList) {
        return super.saveBatch(groupMemberList);
    }

    @Override
    public Boolean removeOne(String memberId) {
        return super.removeById(memberId);
    }

    @Override
    public Boolean removeByGroupId(String groupId) {
        QueryWrapper<ImGroupMemberPo> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        return super.remove(wrapper);
    }

    @Override
    public Long countByGroupId(String groupId) {
        QueryWrapper<ImGroupMemberPo> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        return super.count(wrapper);
    }
}
