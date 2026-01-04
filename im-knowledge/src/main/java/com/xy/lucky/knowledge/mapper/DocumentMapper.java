package com.xy.lucky.knowledge.mapper;

import com.xy.lucky.knowledge.domain.po.DocumentPo;
import com.xy.lucky.knowledge.domain.po.DocumentVersionPo;
import com.xy.lucky.knowledge.domain.vo.DocumentVersionVo;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentVo toVo(DocumentPo entity);

    DocumentVersionVo toVo(DocumentVersionPo entity);
}
