package io.novafs.storage.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建存储配置请求
 */
@Data
@Schema(description = "创建存储配置请求")
public class CreateStorageRequest {

    @NotBlank(message = "存储平台标识符不能为空")
    @Schema(description = "存储平台标识符", example = "minio")
    private String platformIdentifier;

    @NotBlank(message = "配置数据不能为空")
    @Schema(description = "JSON格式的配置数据")
    private String configData;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;
}
