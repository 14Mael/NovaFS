package io.novafs.rag.service;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.rag.client.EmbeddingClient;
import io.novafs.rag.client.QdrantClient;
import io.novafs.rag.entity.RagDocument;
import io.novafs.rag.enums.RagDocumentStatus;
import io.novafs.rag.mapper.RagChunkMapper;
import io.novafs.rag.mapper.RagDocumentMapper;
import io.novafs.rag.parser.ParserManager;
import io.novafs.rag.service.impl.RagDocumentServiceImpl;
import io.novafs.rag.splitter.TextSplitter;
import io.novafs.rag.vo.DocumentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagDocumentServiceImplTest {

    @Mock
    private ParserManager parserManager;
    @Mock
    private TextSplitter textSplitter;
    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private QdrantClient qdrantClient;
    @Mock
    private RagDocumentMapper documentMapper;
    @Mock
    private RagChunkMapper chunkMapper;

    private RagDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RagDocumentServiceImpl(parserManager, textSplitter, embeddingClient,
                qdrantClient, documentMapper, chunkMapper);
    }

    @Test
    void ingestText_success_indexesDocument() {
        when(parserManager.parse(anyString(), anyString(), any())).thenReturn("文档内容");
        when(textSplitter.split("文档内容")).thenReturn(List.of("片段一", "片段二"));
        when(embeddingClient.embedAll(anyList())).thenReturn(List.of(new float[]{1f}, new float[]{2f}));

        DocumentVO vo = service.ingestText(1L, 100L, "doc.md", "文档内容");

        assertEquals("doc.md", vo.getName());
        verify(documentMapper).insert(any(RagDocument.class));
        verify(qdrantClient).ensureCollection();
        verify(qdrantClient).upsert(anyList());
        verify(chunkMapper, times(2)).insert(any());
        verify(documentMapper).update(any(RagDocument.class));
    }

    @Test
    void ingestText_embeddingFailure_marksFailedAndCleansUp() {
        when(parserManager.parse(anyString(), anyString(), any())).thenReturn("文档内容");
        when(textSplitter.split(anyString())).thenReturn(List.of("片段"));
        when(embeddingClient.embedAll(anyList()))
                .thenThrow(new BaseException(ErrorCode.RAG_EMBEDDING_FAILED, "连接失败"));

        assertThrows(BaseException.class, () -> service.ingestText(1L, 100L, "doc.md", "文档内容"));

        verify(qdrantClient).deleteByFilter(anyList());
        verify(documentMapper).update(any(RagDocument.class));
    }

    @Test
    void delete_success_cleansVectorsAndChunks() {
        RagDocument doc = new RagDocument();
        doc.setId(1L);
        doc.setWorkspaceId(1L);
        when(documentMapper.selectOneById(1L)).thenReturn(doc);

        service.delete(1L);

        verify(chunkMapper).deleteByQuery(any());
        verify(documentMapper).deleteById(1L);
        verify(qdrantClient).deleteByFilter(anyList());
    }

    @Test
    void delete_notFound_throws() {
        when(documentMapper.selectOneById(999L)).thenReturn(null);

        assertThrows(BaseException.class, () -> service.delete(999L));
        verify(qdrantClient, never()).deleteByFilter(anyList());
    }
}