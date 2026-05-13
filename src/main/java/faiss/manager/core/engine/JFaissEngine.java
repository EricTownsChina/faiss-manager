package faiss.manager.core.engine;

import com.vectorsearch.faiss.swig.IDSelectorBatch;
import com.vectorsearch.faiss.swig.Index;
import com.vectorsearch.faiss.swig.IndexFlatIP;
import com.vectorsearch.faiss.swig.IndexFlatL2;
import com.vectorsearch.faiss.swig.IndexIDMap;
import com.vectorsearch.faiss.swig.floatArray;
import com.vectorsearch.faiss.swig.longArray;
import com.vectorsearch.faiss.swig.swigfaiss;
import com.vectorsearch.faiss.utils.IndexHelper;
import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import faiss.manager.core.engine.model.IndexConfig;
import faiss.manager.core.engine.model.IndexStats;
import faiss.manager.core.engine.model.MetricType;
import faiss.manager.core.engine.model.SearchResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 jfaiss-cpu 的默认向量引擎实现
 * <p>
 * 使用 IndexIDMap 包装底层索引，以支持自定义向量 ID 和删除操作。
 * SWIG 生成的 Java API 使用 floatArray/longArray 等包装类型。
 * </p>
 * <p>
 * 注意: jfaiss-cpu 仅支持 Linux x86_64 环境运行。
 * </p>
 */
@Slf4j
public class JFaissEngine implements VectorEngine {

    private final Map<String, Index> indexes = new ConcurrentHashMap<>();
    private final Map<String, IndexConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> idGenerators = new ConcurrentHashMap<>();

    @Override
    public void createIndex(String indexId, IndexConfig config) {
        if (indexes.containsKey(indexId)) {
            throw new FaissManagerException(ErrorCode.INDEX_ALREADY_EXISTS,
                    "索引已存在: " + indexId);
        }
        try {
            Index index = buildIndex(config);
            indexes.put(indexId, index);
            configs.put(indexId, config);
            idGenerators.put(indexId, new AtomicLong(0));
            log.info("Created FAISS index: id={}, type={}, dim={}, metric={}",
                    indexId, config.getIndexType(), config.getDimension(), config.getMetricType());
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.INDEX_CREATE_FAILED,
                    "创建索引失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void loadIndex(String indexId, byte[] indexData, IndexConfig config) {
        if (indexes.containsKey(indexId)) {
            throw new FaissManagerException(ErrorCode.INDEX_ALREADY_LOADED,
                    "索引已加载: " + indexId);
        }
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("faiss_load_", ".index");
            Files.write(tempFile, indexData);

            Index index = swigfaiss.read_index(tempFile.toString());
            indexes.put(indexId, index);
            configs.put(indexId, config);
            idGenerators.put(indexId, new AtomicLong(index.getNtotal()));
            log.info("Loaded FAISS index from bytes: id={}, vectors={}", indexId, index.getNtotal());
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.INDEX_LOAD_FAILED,
                    "加载索引失败: " + e.getMessage(), e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    public List<Long> add(String indexId, float[][] vectors) {
        Index index = getLoadedIndex(indexId);
        int n = vectors.length;

        // 分配 ID
        AtomicLong gen = idGenerators.get(indexId);
        long[] ids = new long[n];
        for (int i = 0; i < n; i++) {
            ids[i] = gen.getAndIncrement();
        }

        try {
            // 使用 IndexHelper 转换为 SWIG 类型
            floatArray fa = IndexHelper.makeFloatArray(vectors);
            longArray la = toLongArray(ids);

            index.add_with_ids(n, fa.cast(), la.cast());

            fa.delete();
            la.delete();

            List<Long> result = new ArrayList<>(n);
            for (long id : ids) {
                result.add(id);
            }
            log.debug("Added {} vectors to index {}", n, indexId);
            return result;
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.VECTOR_ADD_FAILED,
                    "向量添加失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void addWithIds(String indexId, float[][] vectors, long[] ids) {
        Index index = getLoadedIndex(indexId);
        int n = vectors.length;

        try {
            floatArray fa = IndexHelper.makeFloatArray(vectors);
            longArray la = toLongArray(ids);

            index.add_with_ids(n, fa.cast(), la.cast());

            fa.delete();
            la.delete();

            // 更新 ID 生成器
            AtomicLong gen = idGenerators.get(indexId);
            for (long id : ids) {
                gen.updateAndGet(current -> Math.max(current, id + 1));
            }
            log.debug("Added {} vectors with custom ids to index {}", n, indexId);
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.VECTOR_ADD_FAILED,
                    "向量添加失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void remove(String indexId, List<Long> vectorIds) {
        Index index = getLoadedIndex(indexId);
        try {
            long[] ids = vectorIds.stream().mapToLong(Long::longValue).toArray();
            longArray la = toLongArray(ids);

            IDSelectorBatch selector = new IDSelectorBatch(ids.length, la.cast());
            long removed = index.remove_ids(selector);

            selector.delete();
            la.delete();

            log.debug("Removed {} vectors from index {} (requested {})", removed, indexId, vectorIds.size());
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.VECTOR_REMOVE_FAILED,
                    "向量删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResult search(String indexId, float[] queryVector, int topK) {
        Index index = getLoadedIndex(indexId);
        IndexConfig config = configs.get(indexId);

        try {
            floatArray queryFa = toFloatArray(queryVector);
            floatArray distFa = new floatArray(topK);
            longArray labelLa = new longArray(topK);

            index.search(1, queryFa.cast(), topK, distFa.cast(), labelLa.cast());

            float[] distances = IndexHelper.toArray(distFa, topK);
            long[] labels = IndexHelper.toArray(labelLa, topK);

            queryFa.delete();
            distFa.delete();
            labelLa.delete();

            return buildSearchResult(distances, labels, topK, 1, config.getMetricType()).get(0);
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.VECTOR_SEARCH_FAILED,
                    "向量查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SearchResult> batchSearch(String indexId, float[][] queryVectors, int topK) {
        Index index = getLoadedIndex(indexId);
        IndexConfig config = configs.get(indexId);
        int nq = queryVectors.length;

        try {
            floatArray queryFa = IndexHelper.makeFloatArray(queryVectors);
            floatArray distFa = new floatArray(nq * topK);
            longArray labelLa = new longArray(nq * topK);

            index.search(nq, queryFa.cast(), topK, distFa.cast(), labelLa.cast());

            float[] distances = IndexHelper.toArray(distFa, nq * topK);
            long[] labels = IndexHelper.toArray(labelLa, nq * topK);

            queryFa.delete();
            distFa.delete();
            labelLa.delete();

            return buildSearchResult(distances, labels, topK, nq, config.getMetricType());
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.VECTOR_SEARCH_FAILED,
                    "批量向量查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] serialize(String indexId) {
        Index index = getLoadedIndex(indexId);
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("faiss_save_", ".index");
            swigfaiss.write_index(index, tempFile.toString());
            byte[] data = Files.readAllBytes(tempFile);
            log.debug("Serialized index {}, size={} bytes", indexId, data.length);
            return data;
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.INDEX_SAVE_FAILED,
                    "索引序列化失败: " + e.getMessage(), e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    public void unload(String indexId) {
        Index index = indexes.remove(indexId);
        configs.remove(indexId);
        idGenerators.remove(indexId);
        if (index != null) {
            index.delete(); // 释放 native 内存
            log.info("Unloaded FAISS index: {}", indexId);
        }
    }

    @Override
    public IndexStats getStats(String indexId) {
        Index index = getLoadedIndex(indexId);
        IndexConfig config = configs.get(indexId);
        long vectorCount = index.getNtotal();
        int dim = config.getDimension();

        long memorySize = vectorCount * dim * Float.BYTES + vectorCount * Long.BYTES;

        return IndexStats.builder()
                .indexId(indexId)
                .vectorCount(vectorCount)
                .dimension(dim)
                .indexType(config.getIndexType())
                .metricType(config.getMetricType().name())
                .memorySizeBytes(memorySize)
                .build();
    }

    @Override
    public boolean isLoaded(String indexId) {
        return indexes.containsKey(indexId);
    }

    // ======================== Private Methods ========================

    private Index buildIndex(IndexConfig config) {
        int dim = config.getDimension();
        MetricType metric = config.getMetricType();

        Index baseIndex;
        if (metric == MetricType.INNER_PRODUCT || metric == MetricType.COSINE) {
            baseIndex = new IndexFlatIP(dim);
        } else {
            baseIndex = new IndexFlatL2(dim);
        }

        return new IndexIDMap(baseIndex);
    }

    private Index getLoadedIndex(String indexId) {
        Index index = indexes.get(indexId);
        if (index == null) {
            throw new FaissManagerException(ErrorCode.INDEX_NOT_LOADED,
                    "索引未加载: " + indexId);
        }
        return index;
    }

    /**
     * Java float[] → SWIG floatArray
     */
    private floatArray toFloatArray(float[] arr) {
        floatArray fa = new floatArray(arr.length);
        for (int i = 0; i < arr.length; i++) {
            fa.setitem(i, arr[i]);
        }
        return fa;
    }

    /**
     * Java long[] → SWIG longArray
     * 注意: SWIG longArray.setitem 接受 int，对应 C++ 的 idx_t (int64_t)
     */
    private longArray toLongArray(long[] arr) {
        longArray la = new longArray(arr.length);
        for (int i = 0; i < arr.length; i++) {
            la.setitem(i, (int) arr[i]);
        }
        return la;
    }

    private List<SearchResult> buildSearchResult(float[] distances, long[] labels,
                                                  int topK, int nq, MetricType metricType) {
        List<SearchResult> results = new ArrayList<>(nq);
        for (int q = 0; q < nq; q++) {
            List<SearchResult.Match> matches = new ArrayList<>();
            for (int k = 0; k < topK; k++) {
                int idx = q * topK + k;
                long vectorId = labels[idx];
                if (vectorId < 0) {
                    continue;
                }
                float dist = distances[idx];
                float score = computeScore(dist, metricType);
                matches.add(SearchResult.Match.builder()
                        .vectorId(vectorId)
                        .distance(dist)
                        .score(score)
                        .build());
            }
            results.add(SearchResult.builder().matches(matches).build());
        }
        return results;
    }

    private float computeScore(float distance, MetricType metricType) {
        if (metricType == MetricType.INNER_PRODUCT || metricType == MetricType.COSINE) {
            return Math.max(0, Math.min(1, distance));
        } else {
            return 1.0f / (1.0f + distance);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }
}
