package com.xy.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImGroupMemberMapper;
import com.xy.database.service.ImGroupMemberService;
import com.xy.domain.po.ImGroupMemberPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group_member】的数据库操作Service实现
 */
@Service
public class ImGroupMemberServiceImpl extends ServiceImpl<ImGroupMemberMapper, ImGroupMemberPo>
        implements ImGroupMemberService {

    @Resource
    private ImGroupMemberMapper imGroupMemberMapper;


    @Override
    public List<String> getNinePeopleAvatar(String groupId) {
        return imGroupMemberMapper.getNinePeopleAvatar(groupId);
    }

    @Override
    public List<ImGroupMemberPo> getGroupMemberList(String groupId) {
        QueryWrapper<ImGroupMemberPo> imGroupMemberPoQueryWrapper = new QueryWrapper<>();
        imGroupMemberPoQueryWrapper.eq("group_id", groupId);
        return this.list(imGroupMemberPoQueryWrapper);
    }

    @Override
    public ImGroupMemberPo getGroupMember(String groupId, String memberId) {
        QueryWrapper<ImGroupMemberPo> imGroupMemberPoQueryWrapper = new QueryWrapper<>();
        imGroupMemberPoQueryWrapper.eq("group_id", groupId);
        imGroupMemberPoQueryWrapper.eq("member_id", memberId);
        return this.getOne(imGroupMemberPoQueryWrapper);
    }

    @Override
    public Boolean insert(ImGroupMemberPo groupMember) {
        return this.insert(groupMember);
    }

    @Override
    public Boolean update(ImGroupMemberPo groupMember) {
        return this.updateById(groupMember);
    }

    @Override
    public Boolean batchInsert(List<ImGroupMemberPo> groupMemberList) {
        return this.batchInsert(groupMemberList);
    }

    @Override
    public Boolean deleteById(String memberId) {
        return this.deleteById(memberId);
    }
}




