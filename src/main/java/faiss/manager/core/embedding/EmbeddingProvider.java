package faiss.manager.core.embedding;

import java.util.List;

/**
 * Embedding 提供者 SPI 接口
 * <p>
 * 定义文本向量化的契约，具体实现可插拔替换。
 * 默认提供 {@link RemoteEmbeddingProvider} 基于远程 API 调用的实现。
 * </p>
 */
public interface EmbeddingProvider {

    /**
     * 单条文本向量化
     *
     * @param text 输入文本
     * @return 向量
     */
    float[] embed(String text);

    /**
     * 批量文本向量化
     *
     * @param texts 输入文本列表
     * @return 向量数组，与输入顺序对应
     */
    float[][] batchEmbed(List<String> texts);

    /**
     * 获取 Embedding 向量维度
     *
     * @return 维度
     */
    int getDimension();
}
