package faiss.manager.service;

import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import faiss.manager.core.engine.VectorEngine;
import faiss.manager.core.metadata.VectorMetadata;
import faiss.manager.core.metadata.VectorMetadataRepository;
import faiss.manager.service.dto.TextEntry;
import faiss.manager.service.dto.VectorEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据管理服务
 * <p>
 * 负责向量数据的增删改，同时维护元数据一致性。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataManageService {

    private final VectorEngine vectorEngine;
    private final EmbeddingService embeddingService;
    private final VectorMetadataRepository metadataRepository;

    /**
     * 批量添加文本（自动 Embedding）
     *
     * @param indexId 索引 ID
     * @param entries 文本条目列表
     * @return 分配的向量 ID 列表
     */
    public List<Long> addTexts(String indexId, List<TextEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量 Embedding
        List<String> texts = entries.stream().map(TextEntry::getText).collect(Collectors.toList());
        float[][] vectors = embeddingService.batchEmbed(texts);

        // 写入向量引擎
        List<Long> vectorIds = vectorEngine.add(indexId, vectors);

        // 写入元数据
        saveMetadata(indexId, entries, vectorIds);

        log.info("Added {} texts to index {}", entries.size(), indexId);
        return vectorIds;
    }

    /**
     * 批量添加已有向量（跳过 Embedding）
     *
     * @param indexId 索引 ID
     * @param entries 向量条目列表
     * @return 分配的向量 ID 列表
     */
    public List<Long> addVectors(String indexId, List<VectorEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        float[][] vectors = entries.stream()
                .map(VectorEntry::getVector)
                .toArray(float[][]::new);

        List<Long> vectorIds = vectorEngine.add(indexId, vectors);

        // 写入元数据
        LocalDateTime now = LocalDateTime.now();
        List<VectorMetadata> metadataList = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            VectorEntry entry = entries.get(i);
            metadataList.add(VectorMetadata.builder()
                    .indexId(indexId)
                    .vectorId(vectorIds.get(i))
                    .documentId(entry.getDocumentId())
                    .text(entry.getText())
                    .extra(entry.getExtra())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        metadataRepository.saveAll(metadataList);

        log.info("Added {} vectors to index {}", entries.size(), indexId);
        return vectorIds;
    }

    /**
     * 按文档 ID 删除（查元数据 → 删向量 → 清元数据）
     *
     * @param indexId    索引 ID
     * @param documentId 业务文档 ID
     */
    public void removeByDocumentId(String indexId, String documentId) {
        List<VectorMetadata> metadataList = metadataRepository
                .findByIndexIdAndDocumentId(indexId, documentId);
        if (metadataList.isEmpty()) {
            throw new FaissManagerException(ErrorCode.DOCUMENT_NOT_FOUND,
                    "文档不存在: indexId=" + indexId + ", documentId=" + documentId);
        }

        List<Long> vectorIds = metadataList.stream()
                .map(VectorMetadata::getVectorId)
                .collect(Collectors.toList());

        // 从引擎删除向量
        try {
            vectorEngine.remove(indexId, vectorIds);
        } catch (FaissManagerException e) {
            if (e.getErrorCode() == ErrorCode.VECTOR_REMOVE_FAILED) {
                log.warn("Engine does not support remove, vectors will be cleaned on rebuild. indexId={}", indexId);
            } else {
                throw e;
            }
        }

        // 清理元数据
        metadataRepository.deleteByIndexIdAndDocumentId(indexId, documentId);
        log.info("Removed document: indexId={}, documentId={}, vectors={}", indexId, documentId, vectorIds.size());
    }

    /**
     * 按向量 ID 列表删除
     *
     * @param indexId   索引 ID
     * @param vectorIds 向量 ID 列表
     */
    public void removeByVectorIds(String indexId, List<Long> vectorIds) {
        try {
            vectorEngine.remove(indexId, vectorIds);
        } catch (FaissManagerException e) {
            if (e.getErrorCode() == ErrorCode.VECTOR_REMOVE_FAILED) {
                log.warn("Engine does not support remove, vectors will be cleaned on rebuild. indexId={}", indexId);
            } else {
                throw e;
            }
        }
        metadataRepository.deleteByIndexIdAndVectorIdIn(indexId, vectorIds);
        log.info("Removed {} vectors from index {}", vectorIds.size(), indexId);
    }

    /**
     * 更新文档（Delete + Re-add 策略）
     *
     * @param indexId    索引 ID
     * @param documentId 业务文档 ID
     * @param newEntries 新的文本条目
     * @return 新分配的向量 ID 列表
     */
    public List<Long> updateDocument(String indexId, String documentId, List<TextEntry> newEntries) {
        // 先删除旧数据
        List<VectorMetadata> oldMetadata = metadataRepository
                .findByIndexIdAndDocumentId(indexId, documentId);
        if (!oldMetadata.isEmpty()) {
            List<Long> oldVectorIds = oldMetadata.stream()
                    .map(VectorMetadata::getVectorId)
                    .collect(Collectors.toList());
            try {
                vectorEngine.remove(indexId, oldVectorIds);
            } catch (FaissManagerException e) {
                if (e.getErrorCode() != ErrorCode.VECTOR_REMOVE_FAILED) {
                    throw e;
                }
                log.warn("Engine does not support remove during update, old vectors remain. indexId={}", indexId);
            }
            metadataRepository.deleteByIndexIdAndDocumentId(indexId, documentId);
        }

        // 添加新数据
        // 确保所有新条目使用同一个 documentId
        newEntries.forEach(e -> e.setDocumentId(documentId));
        return addTexts(indexId, newEntries);
    }

    /**
     * 重建索引（从元数据全量重建，用于碎片整理）
     *
     * @param indexId 索引 ID
     * @param config  索引配置
     */
    public void rebuildIndex(String indexId, faiss.manager.core.engine.model.IndexConfig config) {
        List<VectorMetadata> allMetadata = metadataRepository.findByIndexId(indexId);
        if (allMetadata.isEmpty()) {
            log.warn("No metadata found for rebuild, creating empty index: {}", indexId);
            vectorEngine.unload(indexId);
            vectorEngine.createIndex(indexId, config);
            return;
        }

        log.info("Rebuilding index {} with {} vectors", indexId, allMetadata.size());

        // 需要重新 Embedding 所有文本
        List<String> texts = allMetadata.stream()
                .map(VectorMetadata::getText)
                .collect(Collectors.toList());
        float[][] vectors = embeddingService.batchEmbed(texts);

        // 卸载旧索引，创建新索引
        vectorEngine.unload(indexId);
        vectorEngine.createIndex(indexId, config);

        // 重新添加，保持原来的向量 ID
        long[] ids = allMetadata.stream()
                .mapToLong(VectorMetadata::getVectorId)
                .toArray();
        vectorEngine.addWithIds(indexId, vectors, ids);

        log.info("Index rebuilt: {}, vectors={}", indexId, allMetadata.size());
    }

    /**
     * 查询文档元数据
     */
    public List<VectorMetadata> listDocuments(String indexId) {
        return metadataRepository.findByIndexId(indexId);
    }

    // ======================== Private Methods ========================

    private void saveMetadata(String indexId, List<TextEntry> entries, List<Long> vectorIds) {
        LocalDateTime now = LocalDateTime.now();
        List<VectorMetadata> metadataList = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            TextEntry entry = entries.get(i);
            metadataList.add(VectorMetadata.builder()
                    .indexId(indexId)
                    .vectorId(vectorIds.get(i))
                    .documentId(entry.getDocumentId())
                    .text(entry.getText())
                    .extra(entry.getExtra())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        metadataRepository.saveAll(metadataList);
    }
}
