package io.novafs.rag.client;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat 客户端
 * <p>仅依赖 {@code POST {baseUrl}/chat/completions} 一个端点,兼容 OpenAI / DeepSeek / 通义 / 智谱等。</p>
 */
public class ChatClient {

    private final RestClient restClient;
    private final String model;
    private final double temperature;
    private final int maxTokens;

    public ChatClient(RestClient.Builder builder, String baseUrl, String apiKey, String model, double temperature, int maxTokens) {
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
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

    /** 单轮对话(system + user) */
    public String chat(String systemPrompt, String userMessage) {
        return chat(List.of(
                new Message("system", systemPrompt),
                new Message("user", userMessage)));
    }

    /** 多轮对话 */
    public String chat(List<Message> messages) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens);
        try {
            ChatResponse resp = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) {
                throw new BaseException(ErrorCode.RAG_CHAT_FAILED, "Chat 服务返回为空");
            }
            String content = resp.getChoices().get(0).getMessage().getContent();
            if (content == null) {
                throw new BaseException(ErrorCode.RAG_CHAT_FAILED, "Chat 服务未返回内容");
            }
            return content;
        } catch (RestClientException e) {
            throw new BaseException(ErrorCode.RAG_CHAT_FAILED, "Chat 服务调用失败: " + e.getMessage());
        }
    }

    /** 对话消息 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    /** Chat API 响应模型 */
    @Data
    public static class ChatResponse {
        private List<Choice> choices;

        @Data
        public static class Choice {
            private Message message;
        }
    }
}