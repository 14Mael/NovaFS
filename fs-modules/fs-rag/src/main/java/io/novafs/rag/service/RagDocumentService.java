package io.novafs.rag.service;

import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
import io.novafs.rag.vo.ChunkVO;
import io.novafs.rag.vo.DocumentVO;

import java.io.InputStream;
import java.util.List;

/**
 * RAG 文档管理服务
 */
public interface RagDocumentService {

    /** 文件入库:解析 → 切片 → 向量化 → 写入向量库 */
    DocumentVO ingestFile(Long workspaceId, Long userId, String fileName, String contentType, long size, InputStream in);

    /** 文本直入入库 */
    DocumentVO ingestText(Long workspaceId, Long userId, String name, String content);

    /** 分页查询工作空间内文档 */
    PageResult<DocumentVO> page(Long workspaceId, PageQuery query);

    /** 删除文档(同步清理切片与向量) */
    void delete(Long documentId);

    /** 查询文档切片列表 */
    List<ChunkVO> listChunks(Long documentId);
}