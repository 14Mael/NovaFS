package io.novafs.rag.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 问答结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseVO {

    /** 模型回答 */
    private String answer;

    /** 引用的上下文片段 */
    private List<SearchResultVO> sources;
}