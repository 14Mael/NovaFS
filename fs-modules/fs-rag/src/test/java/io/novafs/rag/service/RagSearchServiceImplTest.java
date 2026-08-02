package io.novafs.rag.service;

import io.novafs.rag.client.EmbeddingClient;
import io.novafs.rag.client.QdrantClient;
import io.novafs.rag.config.RagProperties;
import io.novafs.rag.service.impl.RagSearchServiceImpl;
import io.novafs.rag.vo.SearchResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagSearchServiceImplTest {

    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private QdrantClient qdrantClient;

    private final RagProperties properties = new RagProperties();
    private RagSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        properties.getSearch().setMinScore(0.5);
        service = new RagSearchServiceImpl(embeddingClient, qdrantClient, properties);
    }

    @Test
    void search_filtersByWorkspaceAndMinScore() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{1f, 2f});
        QdrantClient.ScoredPoint p1 = new QdrantClient.ScoredPoint();
        p1.setId("id-1");
        p1.setScore(0.9);
        p1.setPayload(Map.of("documentId", 100L, "documentName", "a.txt", "text", "内容A"));
        QdrantClient.ScoredPoint p2 = new QdrantClient.ScoredPoint();
        p2.setId("id-2");
        p2.setScore(0.1);
        p2.setPayload(Map.of("documentId", 101L, "documentName", "b.txt", "text", "内容B"));
        when(qdrantClient.search(any(float[].class), anyInt(), anyList())).thenReturn(List.of(p1, p2));

        List<SearchResultVO> result = service.search(1L, "查询什么", null);

        assertEquals(1, result.size());
        assertEquals("a.txt", result.get(0).getDocumentName());
        assertEquals(100L, result.get(0).getDocumentId());
        verify(qdrantClient).search(any(float[].class), eq(properties.getSearch().getTopK()),
                eq(List.of(new QdrantClient.FilterCondition("workspaceId", 1L))));
    }

    @Test
    void search_usesExplicitTopK() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{1f});
        when(qdrantClient.search(any(float[].class), anyInt(), anyList())).thenReturn(List.of());

        service.search(1L, "q", 3);

        verify(qdrantClient).search(any(float[].class), eq(3), anyList());
    }
}