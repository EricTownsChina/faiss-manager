package faiss.manager.controller;

import faiss.manager.common.ApiResponse;
import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import faiss.manager.controller.request.BatchSearchRequest;
import faiss.manager.controller.request.TextSearchRequest;
import faiss.manager.controller.request.VectorSearchRequest;
import faiss.manager.service.SearchService;
import faiss.manager.service.dto.EnrichedSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 向量搜索接口
 */
@RestController
@RequestMapping("/api/v1/indexes/{indexId}")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 向量查询
     */
    @PostMapping("/search")
    public ApiResponse<EnrichedSearchResult> searchByVector(
            @PathVariable String indexId,
            @Valid @RequestBody VectorSearchRequest request) {
        EnrichedSearchResult result = searchService.searchByVector(
                indexId, request.getVector(), request.getTopK());
        return ApiResponse.success(result);
    }

    /**
     * 文本语义查询
     */
    @PostMapping("/search/text")
    public ApiResponse<EnrichedSearchResult> searchByText(
            @PathVariable String indexId,
            @Valid @RequestBody TextSearchRequest request) {
        EnrichedSearchResult result = searchService.searchByText(
                indexId, request.getText(), request.getTopK());
        return ApiResponse.success(result);
    }

    /**
     * 批量查询
     */
    @PostMapping("/search/batch")
    public ApiResponse<List<EnrichedSearchResult>> batchSearch(
            @PathVariable String indexId,
            @Valid @RequestBody BatchSearchRequest request) {
        List<EnrichedSearchResult> results;
        if (request.getVectors() != null && !request.getVectors().isEmpty()) {
            float[][] vectors = request.getVectors().toArray(new float[0][]);
            results = searchService.batchSearchByVector(indexId, vectors, request.getTopK());
        } else if (request.getTexts() != null && !request.getTexts().isEmpty()) {
            results = searchService.batchSearchByText(indexId, request.getTexts(), request.getTopK());
        } else {
            throw new FaissManagerException(ErrorCode.PARAM_INVALID, "vectors 或 texts 至少提供一个");
        }
        return ApiResponse.success(results);
    }
}
