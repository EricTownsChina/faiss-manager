package faiss.manager.core.engine.model;

/**
 * 距离度量类型
 */
public enum MetricType {

    /**
     * L2 欧氏距离（值越小越相似）
     */
    L2,

    /**
     * 内积（值越大越相似）
     */
    INNER_PRODUCT,

    /**
     * 余弦相似度（值越大越相似，内部转为内积 + 归一化）
     */
    COSINE
}
