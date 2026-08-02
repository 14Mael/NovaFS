package io.novafs.rag.client;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Embedding 客户端
 * <p>仅依赖 {@code POST {baseUrl}/embeddings} 一个端点,兼容 OpenAI / DeepSeek / 通义 / 智谱等。</p>
 */
public class EmbeddingClient {

    private final RestClient restClient;
    private final String model;
    private final int dimensions;

    public EmbeddingClient(RestClient.Builder builder, String baseUrl, String apiKey, String model, int dimensions) {
        this.model = model;
        this.dimensions = dimensions;
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

    /** 单条文本向量化 */
    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    /** 批量文本向量化,返回顺序与入参一致 */
    public List<float[]> embedAll(List<String> texts) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", texts,
                "dimensions", dimensions);
        try {
            EmbeddingResponse resp = restClient.post()
                    .uri("/embeddings")
                    .body(body)
                    .retrieve()
                    .body(EmbeddingResponse.class);
            if (resp == null || resp.getData() == null) {
                throw new BaseException(ErrorCode.RAG_EMBEDDING_FAILED, "Embedding 服务返回为空");
            }
            List<float[]> result = new ArrayList<>(resp.getData().size());
            for (EmbeddingResponse.EmbeddingData d : resp.getData()) {
                result.add(toFloatArray(d.getEmbedding()));
            }
            return result;
        } catch (RestClientException e) {
            throw new BaseException(ErrorCode.RAG_EMBEDDING_FAILED, "Embedding 服务调用失败: " + e.getMessage());
        }
    }

    private static float[] toFloatArray(double[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (float) values[i];
        }
        return result;
    }

    /** Embedding API 响应模型 */
    @Getter
    public static class EmbeddingResponse {
        private List<EmbeddingData> data;

        @Getter
        public static class EmbeddingData {
            private int index;
            private double[] embedding;
        }
    }
}