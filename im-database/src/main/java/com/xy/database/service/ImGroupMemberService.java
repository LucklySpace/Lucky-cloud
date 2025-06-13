package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImGroupMemberPo;

import java.util.List;


/**
 * @author dense
 * @description 针对表【im_group_member】的数据库操作Service
 */
public interface ImGroupMemberService extends IService<ImGroupMemberPo> {

    List<String> getNinePeopleAvatar(String groupId);

}
