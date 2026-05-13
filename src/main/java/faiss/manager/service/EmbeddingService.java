package faiss.manager.service;

import faiss.manager.core.embedding.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding 编排服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingProvider embeddingProvider;

    /**
     * 单条文本向量化
     */
    public float[] embed(String text) {
        return embeddingProvider.embed(text);
    }

    /**
     * 批量文本向量化
     */
    public float[][] batchEmbed(List<String> texts) {
        return embeddingProvider.batchEmbed(texts);
    }

    /**
     * 获取 Embedding 维度
     */
    public int getDimension() {
        return embeddingProvider.getDimension();
    }
}
