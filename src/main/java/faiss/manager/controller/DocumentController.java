package faiss.manager.controller;

import faiss.manager.common.ApiResponse;
import faiss.manager.core.metadata.VectorMetadata;
import faiss.manager.service.DataManageService;
import faiss.manager.service.dto.TextEntry;
import faiss.manager.service.dto.VectorEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 文档管理接口
 */
@RestController
@RequestMapping("/api/v1/indexes/{indexId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DataManageService dataManageService;

    /**
     * 添加文本（自动 Embedding）
     */
    @PostMapping
    public ApiResponse<List<Long>> addTexts(
            @PathVariable String indexId,
            @Valid @RequestBody List<TextEntry> entries) {
        List<Long> vectorIds = dataManageService.addTexts(indexId, entries);
        return ApiResponse.success(vectorIds);
    }

    /**
     * 添加已有向量
     */
    @PostMapping("/vectors")
    public ApiResponse<List<Long>> addVectors(
            @PathVariable String indexId,
            @Valid @RequestBody List<VectorEntry> entries) {
        List<Long> vectorIds = dataManageService.addVectors(indexId, entries);
        return ApiResponse.success(vectorIds);
    }

    /**
     * 删除文档及其向量
     */
    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> removeDocument(
            @PathVariable String indexId,
            @PathVariable String documentId) {
        dataManageService.removeByDocumentId(indexId, documentId);
        return ApiResponse.success();
    }

    /**
     * 更新文档
     */
    @PutMapping("/{documentId}")
    public ApiResponse<List<Long>> updateDocument(
            @PathVariable String indexId,
            @PathVariable String documentId,
            @Valid @RequestBody List<TextEntry> entries) {
        List<Long> vectorIds = dataManageService.updateDocument(indexId, documentId, entries);
        return ApiResponse.success(vectorIds);
    }

    /**
     * 列出文档元数据
     */
    @GetMapping
    public ApiResponse<List<VectorMetadata>> listDocuments(@PathVariable String indexId) {
        return ApiResponse.success(dataManageService.listDocuments(indexId));
    }
}
