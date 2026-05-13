package faiss.manager.core.engine;

import faiss.manager.core.engine.model.IndexConfig;
import faiss.manager.core.engine.model.IndexStats;
import faiss.manager.core.engine.model.SearchResult;

import java.util.List;

/**
 * 向量引擎 SPI 接口
 * <p>
 * 定义所有向量操作的契约，具体实现可插拔替换。
 * 默认提供 {@link JFaissEngine} 基于 jfaiss-cpu 的实现。
 * </p>
 */
public interface VectorEngine {

    /**
     * 创建空索引
     *
     * @param indexId 索引唯一标识
     * @param config  索引配置
     */
    void createIndex(String indexId, IndexConfig config);

    /**
     * 从字节数据加载索引
     *
     * @param indexId   索引唯一标识
     * @param indexData 序列化的索引数据
     * @param config    索引配置（用于记录元信息）
     */
    void loadIndex(String indexId, byte[] indexData, IndexConfig config);

    /**
     * 添加向量，返回分配的向量 ID 列表
     *
     * @param indexId 索引 ID
     * @param vectors 二维向量数组，每行一个向量
     * @return 分配的向量 ID 列表
     */
    List<Long> add(String indexId, float[][] vectors);

    /**
     * 添加带指定 ID 的向量
     *
     * @param indexId 索引 ID
     * @param vectors 二维向量数组
     * @param ids     指定的向量 ID
     */
    void addWithIds(String indexId, float[][] vectors, long[] ids);

    /**
     * 按 ID 删除向量
     *
     * @param indexId   索引 ID
     * @param vectorIds 要删除的向量 ID 列表
     */
    void remove(String indexId, List<Long> vectorIds);

    /**
     * KNN 查询
     *
     * @param indexId     索引 ID
     * @param queryVector 查询向量
     * @param topK        返回最近邻数量
     * @return 搜索结果
     */
    SearchResult search(String indexId, float[] queryVector, int topK);

    /**
     * 批量 KNN 查询
     *
     * @param indexId      索引 ID
     * @param queryVectors 查询向量数组
     * @param topK         每个查询返回的最近邻数量
     * @return 搜索结果列表
     */
    List<SearchResult> batchSearch(String indexId, float[][] queryVectors, int topK);

    /**
     * 序列化索引为字节数据
     *
     * @param indexId 索引 ID
     * @return 序列化的字节数据
     */
    byte[] serialize(String indexId);

    /**
     * 卸载索引，释放内存
     *
     * @param indexId 索引 ID
     */
    void unload(String indexId);

    /**
     * 获取索引统计信息
     *
     * @param indexId 索引 ID
     * @return 统计信息
     */
    IndexStats getStats(String indexId);

    /**
     * 检查索引是否已加载到内存
     *
     * @param indexId 索引 ID
     * @return 是否已加载
     */
    boolean isLoaded(String indexId);
}
