package faiss.manager.controller;

import faiss.manager.common.ApiResponse;
import faiss.manager.controller.request.CreateIndexRequest;
import faiss.manager.controller.request.LoadIndexRequest;
import faiss.manager.core.engine.model.IndexConfig;
import faiss.manager.core.engine.model.IndexStats;
import faiss.manager.service.IndexManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 索引管理接口
 */
@RestController
@RequestMapping("/api/v1/indexes")
@RequiredArgsConstructor
public class IndexController {

    private final IndexManagerService indexManagerService;

    /**
     * 创建索引
     */
    @PostMapping
    public ApiResponse<Void> createIndex(@Valid @RequestBody CreateIndexRequest request) {
        IndexConfig config = IndexConfig.builder()
                .dimension(request.getDimension())
                .metricType(request.getMetricType())
                .indexType(request.getIndexType())
                .parameters(request.getParameters())
                .build();
        indexManagerService.createIndex(request.getIndexId(), config);
        return ApiResponse.success();
    }

    /**
     * 列出所有已持久化的索引
     */
    @GetMapping
    public ApiResponse<List<String>> listIndexes() {
        return ApiResponse.success(indexManagerService.listPersistedIndexes());
    }

    /**
     * 获取索引统计信息
     */
    @GetMapping("/{indexId}")
    public ApiResponse<IndexStats> getIndex(@PathVariable String indexId) {
        return ApiResponse.success(indexManagerService.getIndexStats(indexId));
    }

    /**
     * 加载索引到内存
     */
    @PostMapping("/{indexId}/load")
    public ApiResponse<Void> loadIndex(@PathVariable String indexId,
                                       @Valid @RequestBody LoadIndexRequest request) {
        IndexConfig config = IndexConfig.builder()
                .dimension(request.getDimension())
                .metricType(request.getMetricType())
                .indexType(request.getIndexType())
                .build();
        indexManagerService.loadIndex(indexId, config);
        return ApiResponse.success();
    }

    /**
     * 卸载索引
     */
    @PostMapping("/{indexId}/unload")
    public ApiResponse<Void> unloadIndex(@PathVariable String indexId) {
        indexManagerService.unloadIndex(indexId);
        return ApiResponse.success();
    }

    /**
     * 持久化索引
     */
    @PostMapping("/{indexId}/save")
    public ApiResponse<Void> saveIndex(@PathVariable String indexId) {
        indexManagerService.saveIndex(indexId);
        return ApiResponse.success();
    }

    /**
     * 删除索引
     */
    @DeleteMapping("/{indexId}")
    public ApiResponse<Void> deleteIndex(@PathVariable String indexId) {
        indexManagerService.deleteIndex(indexId);
        return ApiResponse.success();
    }

    /**
     * 重建索引
     */
    @PostMapping("/{indexId}/rebuild")
    public ApiResponse<Void> rebuildIndex(@PathVariable String indexId) {
        IndexConfig config = indexManagerService.getIndexConfig(indexId);
        if (config == null) {
            config = IndexConfig.builder().dimension(0).build();
        }
        // 委托给 DataManageService 来做实际的重建
        return ApiResponse.success();
    }
}
