package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImGroupMemberMapper;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.rpc.api.database.group.ImGroupMemberDubboService;
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
        Wrapper<ImGroupMemberPo> queryWrapper = Wrappers.<ImGroupMemberPo>lambdaQuery()
                .eq(ImGroupMemberPo::getGroupId, groupId);
        return super.list(queryWrapper);
    }

    @Override
    public ImGroupMemberPo queryOne(String groupId, String memberId) {
        Wrapper<ImGroupMemberPo> queryWrapper = Wrappers.<ImGroupMemberPo>lambdaQuery()
                .eq(ImGroupMemberPo::getGroupId, groupId)
                .eq(ImGroupMemberPo::getMemberId, memberId);
        return super.getOne(queryWrapper);
    }

    @Override
    public List<ImGroupMemberPo> queryByRole(String groupId, Integer role) {
        Wrapper<ImGroupMemberPo> queryWrapper = Wrappers.<ImGroupMemberPo>lambdaQuery()
                .eq(ImGroupMemberPo::getGroupId, groupId)
                .eq(ImGroupMemberPo::getRole, role);
        return super.list(queryWrapper);
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
        Wrapper<ImGroupMemberPo> queryWrapper = Wrappers.<ImGroupMemberPo>lambdaQuery()
                .eq(ImGroupMemberPo::getGroupId, groupId);
        return super.remove(queryWrapper);
    }

    @Override
    public Long countByGroupId(String groupId) {
        Wrapper<ImGroupMemberPo> queryWrapper = Wrappers.<ImGroupMemberPo>lambdaQuery()
                .eq(ImGroupMemberPo::getGroupId, groupId);
        return super.count(queryWrapper);
    }
}
