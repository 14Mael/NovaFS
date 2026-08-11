package io.novafs.rag.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 语义检索结果
 */
@Data
public class SearchResultVO {

    /** Qdrant 点 ID */
    private String pointId;

    /** 文档 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long documentId;

    /** 文档名称 */
    private String documentName;

    /** 相似度分数（0~1） */
    private Double score;

    /** 命中片段内容 */
    private String content;
}
