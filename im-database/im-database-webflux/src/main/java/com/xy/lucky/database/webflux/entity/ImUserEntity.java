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
@Table("im_user")
@Schema(description = "用户表")
public class ImUserEntity {

    @Id
    @Schema(description = "用户ID")
    @Column("user_id")
    private String userId;

    @Schema(description = "用户名")
    @Column("user_name")
    private String userName;

    @Schema(description = "密码")
    @Column("password")
    private String password;

    @Schema(description = "手机号")
    @Column("mobile")
    private String mobile;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "删除标志")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "版本号")
    @Column("version")
    private Integer version;
}
