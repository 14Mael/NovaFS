package io.novafs.rag.service;

import io.novafs.rag.vo.SearchResultVO;

import java.util.List;

/**
 * 语义检索服务
 */
public interface RagSearchService {

    /** 在工作空间内按语义检索文档片段 */
    List<SearchResultVO> search(Long workspaceId, String query, Integer topK);
}