package com.xy.lucky.auth.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 二维码扫码状态响应
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class IMQRCodeResult {

    @Schema(description = "二维码凭证 ID，用于后续查询认证状态")
    private String code;

    @Schema(description = "二维码状态: wait=待扫码, scanned=已扫码, confirmed=已确认, expired=已过期")
    private String status;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "二维码图片（可选）")
    private String imageBase64;

    @Schema(description = "二维码过期时间戳")
    private long expireAt;

    @Schema(description = "附加信息（可选，用于前端展示）")
    private Map<String, Object> extra;
}