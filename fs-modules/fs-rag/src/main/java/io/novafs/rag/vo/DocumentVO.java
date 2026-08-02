package io.novafs.rag.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 文档视图
 */
@Data
public class DocumentVO {

    private Long id;

    private Long workspaceId;

    private Long userId;

    private String name;

    private String contentType;

    private Long size;

    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}