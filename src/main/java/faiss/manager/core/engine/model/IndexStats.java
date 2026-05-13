package faiss.manager.core.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 索引统计信息
 */
@Data
@Builder
public class IndexStats {

    /**
     * 索引 ID
     */
    private String indexId;

    /**
     * 当前向量总数
     */
    private long vectorCount;

    /**
     * 向量维度
     */
    private int dimension;

    /**
     * 索引类型
     */
    private String indexType;

    /**
     * 距离度量类型
     */
    private String metricType;

    /**
     * 预估内存占用（字节）
     */
    private long memorySizeBytes;
}
