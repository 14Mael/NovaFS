package io.novafs.storage.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 存储配置响应
 */
@Data
@Schema(description = "存储配置信息")
public class StorageSettingsResponse {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "存储平台标识符")
    private String platformIdentifier;

    @Schema(description = "配置数据")
    private String configData;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "所属工作空间ID")
    private Long workspaceId;

    @Schema(description = "备注")
    private String remark;
}
