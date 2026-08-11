package io.novafs.system.workspace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间列表响应
 */
@Data
@Schema(description = "工作空间信息")
public class WorkspaceResponse {

    @Schema(description = "工作空间ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "工作空间名称")
    private String name;

    @Schema(description = "URL友好标识")
    private String slug;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "所有者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
