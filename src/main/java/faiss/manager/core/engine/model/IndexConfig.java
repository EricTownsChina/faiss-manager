package faiss.manager.core.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 索引配置
 */
@Data
@Builder
public class IndexConfig {

    /**
     * 向量维度
     */
    private int dimension;

    /**
     * 距离度量类型
     */
    @Builder.Default
    private MetricType metricType = MetricType.L2;

    /**
     * 索引类型，自由字符串，由具体引擎实现解释
     * 例如: "FLAT", "HNSW", "IVF_FLAT", "IDMap,Flat" 等
     */
    @Builder.Default
    private String indexType = "IDMap,Flat";

    /**
     * 引擎特定参数
     * 例如 HNSW 的 M、efConstruction，IVF 的 nlist 等
     */
    private Map<String, Object> parameters;
}
