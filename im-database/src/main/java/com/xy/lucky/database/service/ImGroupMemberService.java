package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImGroupMemberMapper;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.dubbo.api.database.group.ImGroupMemberDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImGroupMemberService extends ServiceImpl<ImGroupMemberMapper, ImGroupMemberPo>
        implements ImGroupMemberDubboService, IService<ImGroupMemberPo> {

    @Resource
    private ImGroupMemberMapper imGroupMemberMapper;

    public List<ImGroupMemberPo> selectList(String groupId) {
        QueryWrapper<ImGroupMemberPo> imGroupMemberPoQueryWrapper = new QueryWrapper<>();
        imGroupMemberPoQueryWrapper.eq("group_id", groupId);
        return this.list(imGroupMemberPoQueryWrapper);
    }

    public ImGroupMemberPo selectOne(String groupId, String memberId) {
        QueryWrapper<ImGroupMemberPo> imGroupMemberPoQueryWrapper = new QueryWrapper<>();
        imGroupMemberPoQueryWrapper.eq("group_id", groupId);
        imGroupMemberPoQueryWrapper.eq("member_id", memberId);
        return this.getOne(imGroupMemberPoQueryWrapper);
    }

    public List<String> selectNinePeopleAvatar(String groupId) {
        return imGroupMemberMapper.selectNinePeopleAvatar(groupId);
    }

    public Boolean insert(ImGroupMemberPo groupMember) {
        return this.save(groupMember);
    }

    public Boolean update(ImGroupMemberPo groupMember) {
        return this.updateById(groupMember);
    }

    public Boolean batchInsert(List<ImGroupMemberPo> groupMemberList) {
        return !imGroupMemberMapper.insert(groupMemberList).isEmpty();
    }

    public Boolean deleteById(String memberId) {
        return this.removeById(memberId);
    }
}