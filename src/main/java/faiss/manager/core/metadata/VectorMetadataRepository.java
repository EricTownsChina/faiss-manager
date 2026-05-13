package faiss.manager.core.metadata;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 向量元数据 MongoDB Repository
 */
@Repository
public interface VectorMetadataRepository extends MongoRepository<VectorMetadata, String> {

    /**
     * 按索引 ID 和文档 ID 查询
     */
    List<VectorMetadata> findByIndexIdAndDocumentId(String indexId, String documentId);

    /**
     * 按索引 ID 和向量 ID 列表查询
     */
    List<VectorMetadata> findByIndexIdAndVectorIdIn(String indexId, List<Long> vectorIds);

    /**
     * 按索引 ID 查询所有元数据
     */
    List<VectorMetadata> findByIndexId(String indexId);

    /**
     * 删除指定索引的所有元数据
     */
    void deleteByIndexId(String indexId);

    /**
     * 删除指定索引下指定文档的所有元数据
     */
    void deleteByIndexIdAndDocumentId(String indexId, String documentId);

    /**
     * 删除指定索引下指定向量 ID 的元数据
     */
    void deleteByIndexIdAndVectorIdIn(String indexId, List<Long> vectorIds);

    /**
     * 统计指定索引的元数据数量
     */
    long countByIndexId(String indexId);
}
