package com.xy.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * IM 连接元数据
 */
@Data
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IMConnectEndpointMetadata {

    @Schema(description = "区域")
    private String region;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "端点")
    private String endpoint;

    @Schema(description = "协议", example = "[\"websocket\"]")
    private List<String> protocols;

    @Schema(description = "创建时间戳")
    private Long createdAt;

}
