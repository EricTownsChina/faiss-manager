package faiss.manager.core.storage;

import java.util.List;

/**
 * 索引文件持久化 SPI 接口
 * <p>
 * 定义索引文件存储的契约。
 * 提供 {@link LocalIndexRepository} 和 {@link S3IndexRepository} 两种实现。
 * </p>
 */
public interface IndexRepository {

    /**
     * 保存索引数据
     *
     * @param indexId 索引 ID
     * @param data    序列化的索引字节数据
     */
    void save(String indexId, byte[] data);

    /**
     * 加载索引数据
     *
     * @param indexId 索引 ID
     * @return 序列化的索引字节数据
     */
    byte[] load(String indexId);

    /**
     * 删除索引数据
     *
     * @param indexId 索引 ID
     */
    void delete(String indexId);

    /**
     * 检查索引是否存在
     *
     * @param indexId 索引 ID
     * @return 是否存在
     */
    boolean exists(String indexId);

    /**
     * 列出所有已持久化的索引 ID
     *
     * @return 索引 ID 列表
     */
    List<String> listAll();
}
