package faiss.manager.service;

import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import faiss.manager.config.FaissManagerProperties;
import faiss.manager.core.engine.VectorEngine;
import faiss.manager.core.engine.model.IndexConfig;
import faiss.manager.core.engine.model.IndexStats;
import faiss.manager.core.metadata.VectorMetadataRepository;
import faiss.manager.core.storage.IndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 索引生命周期管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexManagerService {

    private final VectorEngine vectorEngine;
    private final IndexRepository indexRepository;
    private final VectorMetadataRepository metadataRepository;
    private final FaissManagerProperties properties;

    /**
     * 索引配置缓存：indexId -> IndexConfig
     */
    private final Map<String, IndexConfig> indexConfigCache = new ConcurrentHashMap<>();

    /**
     * 创建空索引
     */
    public void createIndex(String indexId, IndexConfig config) {
        vectorEngine.createIndex(indexId, config);
        indexConfigCache.put(indexId, config);
        log.info("Index created: {}", indexId);
    }

    /**
     * 从存储加载索引到内存
     */
    public void loadIndex(String indexId, IndexConfig config) {
        if (vectorEngine.isLoaded(indexId)) {
            throw new FaissManagerException(ErrorCode.INDEX_ALREADY_LOADED,
                    "索引已加载: " + indexId);
        }
        byte[] data = indexRepository.load(indexId);
        vectorEngine.loadIndex(indexId, data, config);
        indexConfigCache.put(indexId, config);
        log.info("Index loaded from storage: {}", indexId);
    }

    /**
     * 卸载索引释放内存
     */
    public void unloadIndex(String indexId) {
        vectorEngine.unload(indexId);
        indexConfigCache.remove(indexId);
        log.info("Index unloaded: {}", indexId);
    }

    /**
     * 持久化索引到存储
     */
    public void saveIndex(String indexId) {
        byte[] data = vectorEngine.serialize(indexId);
        indexRepository.save(indexId, data);
        log.info("Index saved to storage: {}, size={} bytes", indexId, data.length);
    }

    /**
     * 删除索引（内存 + 存储 + 元数据）
     */
    public void deleteIndex(String indexId) {
        // 卸载内存
        if (vectorEngine.isLoaded(indexId)) {
            vectorEngine.unload(indexId);
        }
        // 删除存储
        if (indexRepository.exists(indexId)) {
            indexRepository.delete(indexId);
        }
        // 清理元数据
        metadataRepository.deleteByIndexId(indexId);
        indexConfigCache.remove(indexId);
        log.info("Index deleted completely: {}", indexId);
    }

    /**
     * 获取索引统计信息
     */
    public IndexStats getIndexStats(String indexId) {
        return vectorEngine.getStats(indexId);
    }

    /**
     * 列出所有已持久化的索引
     */
    public List<String> listPersistedIndexes() {
        return indexRepository.listAll();
    }

    /**
     * 检查索引是否已加载
     */
    public boolean isLoaded(String indexId) {
        return vectorEngine.isLoaded(indexId);
    }

    /**
     * 获取索引配置
     */
    public IndexConfig getIndexConfig(String indexId) {
        return indexConfigCache.get(indexId);
    }

    /**
     * 索引预热：应用启动后自动加载配置中指定的索引
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupIndexes() {
        FaissManagerProperties.WarmupProperties warmup = properties.getWarmup();
        if (!warmup.isEnabled() || warmup.getIndexIds().isEmpty()) {
            return;
        }
        log.info("Starting index warmup for: {}", warmup.getIndexIds());
        for (String indexId : warmup.getIndexIds()) {
            try {
                if (!vectorEngine.isLoaded(indexId) && indexRepository.exists(indexId)) {
                    // 预热时使用默认配置，实际使用中可考虑从元数据存储中读取配置
                    IndexConfig config = indexConfigCache.getOrDefault(indexId,
                            IndexConfig.builder().dimension(0).build());
                    byte[] data = indexRepository.load(indexId);
                    vectorEngine.loadIndex(indexId, data, config);
                    log.info("Warmup loaded index: {}", indexId);
                }
            } catch (Exception e) {
                log.error("Failed to warmup index: {}", indexId, e);
            }
        }
    }
}
