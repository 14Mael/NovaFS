package io.novafs.rag.vo;

import lombok.Data;

/**
 * RAG 文档切片视图
 */
@Data
public class ChunkVO {

    private Long id;

    private Integer chunkIndex;

    private String content;

    private Integer tokenCount;
}