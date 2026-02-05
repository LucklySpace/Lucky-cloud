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
@Table("im_user_data")
@Schema(description = "用户资料实体")
public class ImUserDataEntity {

    @Id
    @Schema(description = "用户ID")
    @Column("user_id")
    private String userId;

    @Schema(description = "昵称")
    @Column("name")
    private String name;

    @Schema(description = "头像")
    @Column("avatar")
    private String avatar;

    @Schema(description = "性别")
    @Column("gender")
    private Integer gender;

    @Schema(description = "生日")
    @Column("birthday")
    private String birthday;

    @Schema(description = "位置")
    @Column("location")
    private String location;

    @Schema(description = "个性签名")
    @Column("self_signature")
    private String selfSignature;

    @Schema(description = "好友申请方式")
    @Column("friend_allow_type")
    private Integer friendAllowType;

    @Schema(description = "封禁标志")
    @Column("forbidden_flag")
    private Integer forbiddenFlag;

    @Schema(description = "禁止加好友")
    @Column("disable_add_friend")
    private Integer disableAddFriend;

    @Schema(description = "静默标志")
    @Column("silent_flag")
    private Integer silentFlag;

    @Schema(description = "用户类型")
    @Column("user_type")
    private Integer userType;

    @Schema(description = "扩展字段")
    @Column("extra")
    private String extra;

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
