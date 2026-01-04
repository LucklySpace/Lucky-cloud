package com.xy.lucky.knowledge.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("doc_group")
@Schema(description = "文档分组")
public class GroupPo {

    @Id
    @Column("id")
    @Schema(description = "分组ID")
    private Long id;

    @Column("name")
    @Schema(description = "分组名称")
    private String name;

    @Column("owner")
    @Schema(description = "所有者")
    private String owner;

    @Column("description")
    @Schema(description = "描述")
    private String description;

    @Column("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
