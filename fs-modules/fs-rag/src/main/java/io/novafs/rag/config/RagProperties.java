package io.novafs.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 模块配置
 * <p>前缀: novafs.rag,包含 embedding / chat / qdrant / splitter / search 五组配置。</p>
 */
@Data
@ConfigurationProperties(prefix = "novafs.rag")
public class RagProperties {

    /** 是否启用 RAG 模块 */
    private boolean enabled = true;

    /** Embedding 模型配置 */
    private final Embedding embedding = new Embedding();

    /** Chat 模型配置 */
    private final Chat chat = new Chat();

    /** Qdrant 向量库配置 */
    private final Qdrant qdrant = new Qdrant();

    /** 文本切片配置 */
    private final Splitter splitter = new Splitter();

    /** 检索配置 */
    private final Search search = new Search();

    @Data
    public static class Embedding {
        /** OpenAI 兼容服务地址,如 https://api.openai.com/v1 */
        private String baseUrl = "https://api.openai.com/v1";
        /** API Key,建议通过环境变量注入 */
        private String apiKey = "";
        /** Embedding 模型名 */
        private String model = "text-embedding-3-small";
        /** 向量维度 */
        private int dimensions = 1536;
    }

    @Data
    public static class Chat {
        /** OpenAI 兼容服务地址 */
        private String baseUrl = "https://api.openai.com/v1";
        /** API Key */
        private String apiKey = "";
        /** 对话模型名 */
        private String model = "gpt-4o-mini";
        /** 采样温度 */
        private double temperature = 0.3;
        /** 最大输出 token 数 */
        private int maxTokens = 1024;
    }

    @Data
    public static class Qdrant {
        /** Qdrant REST 服务地址 */
        private String baseUrl = "http://localhost:6333";
        /** Qdrant API Key(未开启鉴权时留空) */
        private String apiKey = "";
        /** 集合名称 */
        private String collection = "novafs_docs";
        /** 向量维度,需与 embedding 模型一致 */
        private int vectorSize = 1536;
    }

    @Data
    public static class Splitter {
        /** 每个切片的字符数 */
        private int chunkSize = 800;
        /** 相邻切片重叠字符数 */
        private int chunkOverlap = 100;
    }

    @Data
    public static class Search {
        /** 默认返回的片段数 */
        private int topK = 5;
        /** 最低相似度阈值(0~1) */
        private double minScore = 0.0;
    }
}