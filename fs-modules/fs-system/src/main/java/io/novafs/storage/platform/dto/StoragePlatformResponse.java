package io.novafs.storage.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 存储平台响应
 */
@Data
@Schema(description = "存储平台信息")
public class StoragePlatformResponse {

    @Schema(description = "平台ID")
    private Integer id;

    @Schema(description = "平台名称")
    private String name;

    @Schema(description = "平台标识符")
    private String identifier;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否默认")
    private Boolean isDefault;

    @Schema(description = "描述")
    private String description;
}
