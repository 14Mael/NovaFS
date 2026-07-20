package io.novafs.system.workspace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作空间详情响应（含当前用户角色权限）
 */
@Data
@Schema(description = "工作空间详情")
public class WorkspaceDetailResponse {

    @Schema(description = "工作空间ID")
    private Long id;

    @Schema(description = "工作空间名称")
    private String name;

    @Schema(description = "URL友好标识")
    private String slug;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "所有者ID")
    private Long ownerId;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "当前用户角色编码")
    private String roleCode;

    @Schema(description = "当前用户角色名称")
    private String roleName;

    @Schema(description = "当前用户权限列表")
    private List<String> permissions;
}
