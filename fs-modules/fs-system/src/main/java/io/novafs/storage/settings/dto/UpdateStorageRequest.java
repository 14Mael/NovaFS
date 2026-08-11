package io.novafs.storage.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新存储配置请求（字段为空表示保留原值）
 */
@Data
@Schema(description = "更新存储配置请求")
public class UpdateStorageRequest {

    @Schema(description = "JSON格式的配置数据，为空保留原值")
    private String configData;

    @Schema(description = "是否启用，为空保留原值")
    private Boolean enabled;

    @Schema(description = "备注，为空保留原值")
    private String remark;
}
