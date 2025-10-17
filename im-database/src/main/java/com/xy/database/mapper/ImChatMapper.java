package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImChatPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_chat】的数据库操作Mapper
 */
@Mapper
public interface ImChatMapper extends BaseMapper<ImChatPo> {


    List<ImChatPo> selectList(@Param("ownerId") String ownerId, @Param("sequence") Long sequence);
}




