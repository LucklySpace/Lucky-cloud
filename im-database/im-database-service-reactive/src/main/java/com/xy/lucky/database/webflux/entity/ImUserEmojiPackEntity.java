package com.xy.lucky.database.webflux.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("im_user_emoji_pack")
@Schema(description = "用户表情包关联表")
public class ImUserEmojiPackEntity {

    @Id
    @Schema(description = "ID")
    @Column("id")
    private String id;

    @Schema(description = "用户ID")
    @Column("user_id")
    private String userId;

    @Schema(description = "表情包ID")
    @Column("pack_id")
    private String packId;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "删除标识")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "版本信息")
    @Column("version")
    private Integer version;
}
