package faiss.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FAISS Manager 统一配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "faiss-manager")
public class FaissManagerProperties {

    /**
     * 向量引擎类型: jfaiss (默认)
     */
    private String engine = "jfaiss";

    /**
     * 存储配置
     */
    private StorageProperties storage = new StorageProperties();

    /**
     * Embedding 配置
     */
    private EmbeddingProperties embedding = new EmbeddingProperties();

    /**
     * 限流配置
     */
    private RateLimitProperties rateLimit = new RateLimitProperties();

    /**
     * 索引预热配置
     */
    private WarmupProperties warmup = new WarmupProperties();

    @Data
    public static class StorageProperties {
        /**
         * 存储类型: local | s3
         */
        private String type = "local";

        /**
         * 本地存储配置
         */
        private LocalStorageProperties local = new LocalStorageProperties();

        /**
         * S3 存储配置
         */
        private S3StorageProperties s3 = new S3StorageProperties();
    }

    @Data
    public static class LocalStorageProperties {
        /**
         * 索引文件存储目录
         */
        private String baseDir = "./data/indexes";
    }

    @Data
    public static class S3StorageProperties {
        /**
         * S3 Bucket 名称
         */
        private String bucket;

        /**
         * S3 Key 前缀
         */
        private String prefix = "indexes/";

        /**
         * AWS Region
         */
        private String region = "ap-southeast-1";

        /**
         * 自定义 Endpoint（用于 MinIO 等兼容存储）
         */
        private String endpoint;
    }

    @Data
    public static class EmbeddingProperties {
        /**
         * Embedding 提供者类型: remote
         */
        private String provider = "remote";

        /**
         * 远程 Embedding 配置
         */
        private RemoteEmbeddingProperties remote = new RemoteEmbeddingProperties();
    }

    @Data
    public static class RemoteEmbeddingProperties {
        /**
         * 远程 Embedding 服务 URL
         */
        private String url = "http://localhost:8080/api/embed";

        /**
         * 批量请求大小
         */
        private int batchSize = 32;

        /**
         * 向量维度
         */
        private int dimension = 768;

        /**
         * 请求超时时间（毫秒）
         */
        private int timeoutMs = 30000;

        /**
         * 重试次数
         */
        private int retry = 3;
    }

    @Data
    public static class RateLimitProperties {
        /**
         * 是否启用限流
         */
        private boolean enabled = false;

        /**
         * 搜索接口 QPS 限制
         */
        private double searchQps = 100;
    }

    @Data
    public static class WarmupProperties {
        /**
         * 是否启用索引预热
         */
        private boolean enabled = false;

        /**
         * 启动时自动加载的索引 ID 列表
         */
        private java.util.List<String> indexIds = new java.util.ArrayList<>();
    }
}
