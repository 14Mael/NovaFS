package io.novafs.rag.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * RAG 文档切片视图
 */
@Data
public class ChunkVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Integer chunkIndex;

    private String content;

    private Integer tokenCount;
}
