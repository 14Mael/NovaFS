package io.novafs.rag.config;

import io.novafs.rag.client.ChatClient;
import io.novafs.rag.client.EmbeddingClient;
import io.novafs.rag.client.QdrantClient;
import io.novafs.rag.splitter.TextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RAG 模块自动配置
 * <p>通过 {@code novafs.rag.enabled=true}(默认)启用,组装模型客户端与向量库客户端 Bean。</p>
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
@ConditionalOnProperty(prefix = "novafs.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagAutoConfiguration {

    @Bean
    public EmbeddingClient embeddingClient(RestClient.Builder builder, RagProperties props) {
        return new EmbeddingClient(
                builder,
                props.getEmbedding().getBaseUrl(),
                props.getEmbedding().getApiKey(),
                props.getEmbedding().getModel(),
                props.getEmbedding().getDimensions());
    }

    @Bean
    public ChatClient chatClient(RestClient.Builder builder, RagProperties props) {
        return new ChatClient(
                builder,
                props.getChat().getBaseUrl(),
                props.getChat().getApiKey(),
                props.getChat().getModel(),
                props.getChat().getTemperature(),
                props.getChat().getMaxTokens());
    }

    @Bean
    public TextSplitter textSplitter(RagProperties props) {
        return new TextSplitter(props.getSplitter().getChunkSize(), props.getSplitter().getChunkOverlap());
    }

    @Bean
    public QdrantClient qdrantClient(RestClient.Builder builder, RagProperties props) {
        return new QdrantClient(
                builder,
                props.getQdrant().getBaseUrl(),
                props.getQdrant().getApiKey(),
                props.getQdrant().getCollection(),
                props.getQdrant().getVectorSize());
    }
}