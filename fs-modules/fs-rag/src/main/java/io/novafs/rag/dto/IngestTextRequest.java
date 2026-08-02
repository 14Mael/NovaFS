package io.novafs.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文本入库请求
 */
@Data
public class IngestTextRequest {

    @NotNull(message = "工作空间不能为空")
    private Long workspaceId;

    @NotBlank(message = "文档名称不能为空")
    private String name;

    @NotBlank(message = "文档内容不能为空")
    private String content;
}