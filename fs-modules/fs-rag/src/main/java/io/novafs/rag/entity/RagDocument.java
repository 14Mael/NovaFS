package io.novafs.rag.entity;

import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import io.novafs.rag.enums.RagDocumentStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * RAG 文档实体
 */
@Getter
@Setter
@Table("rag_document")
public class RagDocument extends BaseEntity {

    private Long workspaceId;

    private Long userId;

    private String name;

    private String contentType;

    private Long size;

    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    public boolean isIndexed() {
        return status != null && status == RagDocumentStatus.INDEXED.getCode();
    }

    public boolean isFailed() {
        return status != null && status == RagDocumentStatus.FAILED.getCode();
    }

    /** 标记为已索引 */
    public void markIndexed(int chunkCount) {
        this.status = RagDocumentStatus.INDEXED.getCode();
        this.chunkCount = chunkCount;
        this.errorMsg = null;
    }

    /** 标记为失败 */
    public void markFailed(String errorMsg) {
        this.status = RagDocumentStatus.FAILED.getCode();
        this.errorMsg = errorMsg;
    }
}