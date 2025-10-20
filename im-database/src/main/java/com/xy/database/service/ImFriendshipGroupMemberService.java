package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImFriendshipGroupMemberMapper;
import com.xy.domain.po.ImFriendshipGroupMemberPo;
import org.springframework.stereotype.Service;


@Service
public class ImFriendshipGroupMemberService extends ServiceImpl<ImFriendshipGroupMemberMapper, ImFriendshipGroupMemberPo>
        implements IService<ImFriendshipGroupMemberPo> {

}




