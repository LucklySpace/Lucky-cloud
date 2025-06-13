package com.xy.database.mapper;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImChatPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author dense
 * @description 针对表【im_chat】的数据库操作Mapper
 */
@Mapper
public interface ImChatMapper extends BaseMapper<ImChatPo> {

}




