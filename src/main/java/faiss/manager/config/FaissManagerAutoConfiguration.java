package faiss.manager.config;

import faiss.manager.core.embedding.EmbeddingProvider;
import faiss.manager.core.embedding.RemoteEmbeddingProvider;
import faiss.manager.core.engine.JFaissEngine;
import faiss.manager.core.engine.VectorEngine;
import faiss.manager.core.storage.IndexRepository;
import faiss.manager.core.storage.LocalIndexRepository;
import faiss.manager.core.storage.S3IndexRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.nio.file.Paths;

/**
 * FAISS Manager 自动装配配置
 */
@Slf4j
@Configuration
public class FaissManagerAutoConfiguration {

    // ======================== VectorEngine ========================

    /**
     * 默认向量引擎：JFaiss (jfaiss-cpu JNI)
     * 仅在用户未自行注册 VectorEngine Bean 时生效
     */
    @Bean
    @ConditionalOnMissingBean(VectorEngine.class)
    @ConditionalOnProperty(prefix = "faiss-manager", name = "engine", havingValue = "jfaiss", matchIfMissing = true)
    public VectorEngine jFaissEngine() {
        log.info("Initializing JFaiss vector engine (jfaiss-cpu JNI)");
        return new JFaissEngine();
    }

    // ======================== IndexRepository ========================

    @Bean
    @ConditionalOnMissingBean(IndexRepository.class)
    @ConditionalOnProperty(prefix = "faiss-manager.storage", name = "type", havingValue = "local", matchIfMissing = true)
    public IndexRepository localIndexRepository(FaissManagerProperties properties) {
        String baseDir = properties.getStorage().getLocal().getBaseDir();
        log.info("Initializing local index repository: {}", baseDir);
        return new LocalIndexRepository(Paths.get(baseDir));
    }

    @Bean
    @ConditionalOnMissingBean(IndexRepository.class)
    @ConditionalOnProperty(prefix = "faiss-manager.storage", name = "type", havingValue = "s3")
    public IndexRepository s3IndexRepository(FaissManagerProperties properties) {
        FaissManagerProperties.S3StorageProperties s3Props = properties.getStorage().getS3();
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Props.getRegion()));
        if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(s3Props.getEndpoint()));
        }
        S3Client s3Client = builder.build();
        log.info("Initializing S3 index repository: bucket={}, prefix={}", s3Props.getBucket(), s3Props.getPrefix());
        return new S3IndexRepository(s3Client, s3Props.getBucket(), s3Props.getPrefix());
    }

    // ======================== EmbeddingProvider ========================

    @Bean
    @ConditionalOnMissingBean(EmbeddingProvider.class)
    @ConditionalOnProperty(prefix = "faiss-manager.embedding", name = "provider", havingValue = "remote", matchIfMissing = true)
    public EmbeddingProvider remoteEmbeddingProvider(FaissManagerProperties properties) {
        FaissManagerProperties.RemoteEmbeddingProperties remote = properties.getEmbedding().getRemote();
        RestTemplate restTemplate = buildRestTemplate(remote.getTimeoutMs());
        log.info("Initializing remote embedding provider: url={}, dim={}", remote.getUrl(), remote.getDimension());
        return new RemoteEmbeddingProvider(
                remote.getUrl(),
                remote.getBatchSize(),
                remote.getDimension(),
                restTemplate
        );
    }

    private RestTemplate buildRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
