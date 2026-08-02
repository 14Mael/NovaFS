package io.novafs.rag.client;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Qdrant 向量库 REST 客户端
 * <p>封装集合管理、向量写入、相似检索与按 payload 过滤删除,向量距离使用 Cosine。</p>
 */
public class QdrantClient {

    private final RestClient restClient;
    private final String collection;
    private final int vectorSize;

    public QdrantClient(RestClient.Builder builder, String baseUrl, String apiKey, String collection, int vectorSize) {
        this.collection = collection;
        this.vectorSize = vectorSize;
        builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.restClient = builder.build();
    }

    /** 底层 RestClient,供测试绑定 MockRestServiceServer */
    public RestClient restClient() {
        return restClient;
    }

    /** 确保集合存在,不存在则按配置维度创建 */
    public void ensureCollection() {
        if (exists()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vectors", Map.of("size", vectorSize, "distance", "Cosine"));
        try {
            restClient.put()
                    .uri("/collections/{name}", collection)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw wrap(e);
        }
    }

    private boolean exists() {
        try {
            restClient.get()
                    .uri("/collections/{name}", collection)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
            throw wrap(e);
        } catch (RestClientException e) {
            throw wrap(e);
        }
    }

    /** 批量写入向量点,幂等(同 ID 覆盖) */
    public void upsert(List<Point> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Map<String, Object>> pointBodies = new ArrayList<>(points.size());
        for (Point p : points) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", p.id());
            body.put("vector", toList(p.vector()));
            body.put("payload", p.payload());
            pointBodies.add(body);
        }
        try {
            restClient.put()
                    .uri("/collections/{name}/points?wait=true", collection)
                    .body(Map.of("points", pointBodies))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw wrap(e);
        }
    }

    /** 相似检索,conditions 为过滤条件(AND 关系) */
    public List<ScoredPoint> search(float[] vector, int limit, List<FilterCondition> conditions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", toList(vector));
        body.put("limit", limit);
        if (conditions != null && !conditions.isEmpty()) {
            body.put("filter", buildFilter(conditions));
        }
        body.put("with_payload", true);
        try {
            SearchResponse resp = restClient.post()
                    .uri("/collections/{name}/points/search", collection)
                    .body(body)
                    .retrieve()
                    .body(SearchResponse.class);
            if (resp == null || resp.getResult() == null) {
                return List.of();
            }
            return resp.getResult();
        } catch (RestClientException e) {
            throw wrap(e);
        }
    }

    /** 按 payload 过滤条件删除向量点 */
    public void deleteByFilter(List<FilterCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/collections/{name}/points/delete", collection)
                    .body(Map.of("filter", buildFilter(conditions)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw wrap(e);
        }
    }

    private Map<String, Object> buildFilter(List<FilterCondition> conditions) {
        List<Map<String, Object>> must = new ArrayList<>(conditions.size());
        for (FilterCondition c : conditions) {
            must.add(Map.of("key", c.key(), "match", Map.of("value", c.value())));
        }
        return Map.of("must", must);
    }

    private static List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float v : vector) {
            list.add(v);
        }
        return list;
    }

    private static BaseException wrap(RestClientException e) {
        return new BaseException(ErrorCode.RAG_QDRANT_UNAVAILABLE, "Qdrant 调用失败: " + e.getMessage());
    }

    /** 向量点 */
    public record Point(String id, float[] vector, Map<String, Object> payload) {
    }

    /** 过滤条件(key-match value) */
    public record FilterCondition(String key, Object value) {
    }

    /** 检索结果 */
    @Data
    public static class SearchResponse {
        private List<ScoredPoint> result;
    }

    @Data
    public static class ScoredPoint {
        private String id;
        private double score;
        private Map<String, Object> payload;
    }
}