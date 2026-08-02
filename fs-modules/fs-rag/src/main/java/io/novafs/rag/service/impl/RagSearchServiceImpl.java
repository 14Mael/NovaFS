package io.novafs.rag.service.impl;

import io.novafs.rag.client.EmbeddingClient;
import io.novafs.rag.client.QdrantClient;
import io.novafs.rag.config.RagProperties;
import io.novafs.rag.service.RagSearchService;
import io.novafs.rag.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 语义检索服务实现
 */
@Service
@RequiredArgsConstructor
public class RagSearchServiceImpl implements RagSearchService {

    private final EmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;
    private final RagProperties properties;

    @Override
    public List<SearchResultVO> search(Long workspaceId, String query, Integer topK) {
        int limit = topK != null ? topK : properties.getSearch().getTopK();
        float[] vector = embeddingClient.embed(query);
        List<QdrantClient.ScoredPoint> points = qdrantClient.search(
                vector, limit,
                List.of(new QdrantClient.FilterCondition("workspaceId", workspaceId)));
        double minScore = properties.getSearch().getMinScore();
        List<SearchResultVO> result = new ArrayList<>(points.size());
        for (QdrantClient.ScoredPoint p : points) {
            if (p.getScore() < minScore) {
                continue;
            }
            Map<String, Object> payload = p.getPayload();
            SearchResultVO vo = new SearchResultVO();
            vo.setPointId(p.getId());
            vo.setScore(p.getScore());
            vo.setDocumentId(asLong(payload.get("documentId")));
            vo.setDocumentName(asString(payload.get("documentName")));
            vo.setContent(asString(payload.get("text")));
            result.add(vo);
        }
        return result;
    }

    private static Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}