package io.novafs.rag.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
import io.novafs.rag.client.EmbeddingClient;
import io.novafs.rag.client.QdrantClient;
import io.novafs.rag.entity.RagChunk;
import io.novafs.rag.entity.RagDocument;
import io.novafs.rag.enums.RagDocumentStatus;
import io.novafs.rag.mapper.RagChunkMapper;
import io.novafs.rag.mapper.RagDocumentMapper;
import io.novafs.rag.parser.ParserManager;
import io.novafs.rag.service.RagDocumentService;
import io.novafs.rag.splitter.TextSplitter;
import io.novafs.rag.vo.ChunkVO;
import io.novafs.rag.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 文档管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentServiceImpl implements RagDocumentService {

    private static final int MAX_ERROR_MSG = 500;

    private final ParserManager parserManager;
    private final TextSplitter textSplitter;
    private final EmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;
    private final RagDocumentMapper documentMapper;
    private final RagChunkMapper chunkMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO ingestFile(Long workspaceId, Long userId, String fileName, String contentType, long size, InputStream in) {
        return ingest(workspaceId, userId, fileName, contentType, size, in);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO ingestText(Long workspaceId, Long userId, String name, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ingest(workspaceId, userId, name, "text/plain", bytes.length, new ByteArrayInputStream(bytes));
    }

    @Override
    public PageResult<DocumentVO> page(Long workspaceId, PageQuery query) {
        QueryWrapper qw = QueryWrapper.create()
                .where(RagDocument::getWorkspaceId).eq(workspaceId)
                .orderBy(RagDocument::getCreatedAt, false);
        Page<RagDocument> result = documentMapper.paginate(query.getPage(), query.getPageSize(), qw);
        List<DocumentVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(query.getPage(), query.getPageSize(), result.getTotalRow(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long documentId) {
        RagDocument doc = documentMapper.selectOneById(documentId);
        if (doc == null) {
            throw new BaseException(ErrorCode.RAG_DOCUMENT_NOT_FOUND);
        }
        chunkMapper.deleteByQuery(QueryWrapper.create().where(RagChunk::getDocumentId).eq(documentId));
        documentMapper.deleteById(documentId);
        qdrantClient.deleteByFilter(List.of(new QdrantClient.FilterCondition("documentId", documentId)));
    }

    @Override
    public List<ChunkVO> listChunks(Long documentId) {
        List<RagChunk> chunks = chunkMapper.selectListByQuery(QueryWrapper.create()
                .where(RagChunk::getDocumentId).eq(documentId)
                .orderBy(RagChunk::getChunkIndex, true));
        List<ChunkVO> vos = new ArrayList<>(chunks.size());
        for (RagChunk c : chunks) {
            ChunkVO vo = new ChunkVO();
            vo.setId(c.getId());
            vo.setChunkIndex(c.getChunkIndex());
            vo.setContent(c.getContent());
            vo.setTokenCount(c.getTokenCount());
            vos.add(vo);
        }
        return vos;
    }

    private DocumentVO ingest(Long workspaceId, Long userId, String name, String contentType, long size, InputStream in) {
        RagDocument doc = new RagDocument();
        doc.setWorkspaceId(workspaceId);
        doc.setUserId(userId);
        doc.setName(name);
        doc.setContentType(contentType);
        doc.setSize(size);
        doc.setStatus(RagDocumentStatus.PARSING.getCode());
        documentMapper.insert(doc);

        try {
            String text = parserManager.parse(name, contentType, in);
            List<String> chunks = textSplitter.split(text);
            if (chunks.isEmpty()) {
                doc.markFailed("文档内容为空");
                documentMapper.update(doc);
                throw new BaseException(ErrorCode.RAG_DOCUMENT_PARSE_FAILED, "文档内容为空: " + name);
            }
            List<float[]> vectors = embeddingClient.embedAll(chunks);
            qdrantClient.ensureCollection();
            qdrantClient.upsert(buildPoints(doc, name, workspaceId, chunks, vectors));
            saveChunks(doc.getId(), chunks);
            doc.markIndexed(chunks.size());
            documentMapper.update(doc);
            return toVO(doc);
        } catch (BaseException e) {
            markFailedAndCleanup(doc, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("RAG 文档处理失败: {}", name, e);
            markFailedAndCleanup(doc, e.getMessage());
            throw new BaseException(ErrorCode.RAG_DOCUMENT_PARSE_FAILED, "文档处理失败: " + name);
        }
    }

    private List<QdrantClient.Point> buildPoints(RagDocument doc, String name, Long workspaceId,
                                                 List<String> chunks, List<float[]> vectors) {
        List<QdrantClient.Point> points = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("workspaceId", workspaceId);
            payload.put("documentId", doc.getId());
            payload.put("documentName", name);
            payload.put("chunkIndex", i);
            payload.put("text", chunks.get(i));
            String pointId = UUID.nameUUIDFromBytes((doc.getId() + ":" + i).getBytes(StandardCharsets.UTF_8)).toString();
            points.add(new QdrantClient.Point(pointId, vectors.get(i), payload));
        }
        return points;
    }

    private void saveChunks(Long documentId, List<String> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = new RagChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setTokenCount(Math.max(1, chunks.get(i).length() / 4));
            chunkMapper.insert(chunk);
        }
    }

    private void markFailedAndCleanup(RagDocument doc, String errorMsg) {
        try {
            qdrantClient.deleteByFilter(List.of(new QdrantClient.FilterCondition("documentId", doc.getId())));
        } catch (Exception e) {
            log.warn("清理 RAG 向量失败, documentId={}", doc.getId(), e);
        }
        String msg = errorMsg == null ? "未知错误" : errorMsg;
        doc.markFailed(msg.length() > MAX_ERROR_MSG ? msg.substring(0, MAX_ERROR_MSG) : msg);
        documentMapper.update(doc);
    }

    private DocumentVO toVO(RagDocument d) {
        DocumentVO vo = new DocumentVO();
        vo.setId(d.getId());
        vo.setWorkspaceId(d.getWorkspaceId());
        vo.setUserId(d.getUserId());
        vo.setName(d.getName());
        vo.setContentType(d.getContentType());
        vo.setSize(d.getSize());
        vo.setStatus(d.getStatus());
        vo.setChunkCount(d.getChunkCount());
        vo.setErrorMsg(d.getErrorMsg());
        vo.setCreatedAt(d.getCreatedAt());
        vo.setUpdatedAt(d.getUpdatedAt());
        return vo;
    }
}