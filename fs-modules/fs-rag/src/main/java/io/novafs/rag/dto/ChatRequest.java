package io.novafs.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RAG 问答请求
 */
@Data
public class ChatRequest {

    @NotNull(message = "工作空间不能为空")
    private Long workspaceId;

    @NotBlank(message = "问题不能为空")
    private String question;
}