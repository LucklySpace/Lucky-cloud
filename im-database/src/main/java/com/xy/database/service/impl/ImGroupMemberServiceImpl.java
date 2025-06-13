package com.xy.database.service.impl;

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
}




