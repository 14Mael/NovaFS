package io.novafs.rag.enums;

/**
 * RAG 文档状态
 */
public enum RagDocumentStatus {

    /** 解析中 */
    PARSING(0),
    /** 已索引 */
    INDEXED(1),
    /** 解析失败 */
    FAILED(2);

    private final int code;

    RagDocumentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}