package faiss.manager.core.metadata;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 向量元数据 MongoDB 文档
 * <p>
 * 维护向量 ID 与业务数据的映射关系。
 * </p>
 */
@Data
@Builder
@Document(collection = "vector_metadata")
@CompoundIndexes({
        @CompoundIndex(name = "idx_index_vector", def = "{'indexId': 1, 'vectorId': 1}", unique = true),
        @CompoundIndex(name = "idx_index_document", def = "{'indexId': 1, 'documentId': 1}")
})
public class VectorMetadata {

    @Id
    private String id;

    /**
     * 所属索引 ID
     */
    private String indexId;

    /**
     * 向量引擎分配的向量 ID
     */
    private long vectorId;

    /**
     * 业务文档 ID（由调用方定义）
     */
    private String documentId;

    /**
     * 原始文本
     */
    private String text;

    /**
     * 扩展业务数据
     */
    private Map<String, Object> extra;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
