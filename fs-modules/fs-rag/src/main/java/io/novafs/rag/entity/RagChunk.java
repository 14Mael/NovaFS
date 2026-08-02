package io.novafs.rag.entity;

import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * RAG 文档切片实体
 */
@Getter
@Setter
@Table("rag_chunk")
public class RagChunk extends BaseEntity {

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private Integer tokenCount;
}