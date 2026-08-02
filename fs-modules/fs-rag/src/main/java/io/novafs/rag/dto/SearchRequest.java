package io.novafs.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 语义检索请求
 */
@Data
public class SearchRequest {

    @NotNull(message = "工作空间不能为空")
    private Long workspaceId;

    @NotBlank(message = "检索内容不能为空")
    private String query;

    @Min(value = 1, message = "topK 最小为 1")
    @Max(value = 50, message = "topK 最大为 50")
    private Integer topK;
}