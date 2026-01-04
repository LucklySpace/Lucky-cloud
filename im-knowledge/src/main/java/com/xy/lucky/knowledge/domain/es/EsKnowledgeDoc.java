package com.xy.lucky.knowledge.domain.es;

import lombok.Data;
import lombok.experimental.Accessors;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.rely.Analyzer;
import org.dromara.easyes.annotation.rely.FieldType;
import org.dromara.easyes.annotation.rely.IdType;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@IndexName("knowledge_documents")
public class EsKnowledgeDoc {

    @IndexId(type = IdType.CUSTOMIZE)
    private String id;

    @IndexField(fieldType = FieldType.LONG)
    private Long docId;

    @IndexField(fieldType = FieldType.INTEGER)
    private Integer version;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.STANDARD, searchAnalyzer = Analyzer.STANDARD)
    private String title;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.STANDARD, searchAnalyzer = Analyzer.STANDARD)
    private String content;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String filename;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String creator;

    @IndexField(fieldType = FieldType.DATE)
    private LocalDateTime createTime;

    @IndexField(fieldType = FieldType.LONG)
    private Long groupId;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String groupName;
}
