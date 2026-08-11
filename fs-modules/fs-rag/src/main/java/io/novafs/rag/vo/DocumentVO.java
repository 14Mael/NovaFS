package io.novafs.rag.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 文档视图
 */
@Data
public class DocumentVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long workspaceId;

    @JsonSerialize(using = ToStringSerializer.class)
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
