package faiss.manager.controller;

import faiss.manager.common.ApiResponse;
import faiss.manager.controller.request.EmbeddingRequest;
import faiss.manager.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Embedding 接口（独立调用）
 */
@RestController
@RequestMapping("/api/v1/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    /**
     * 文本转向量
     */
    @PostMapping
    public ApiResponse<float[][]> embed(@Valid @RequestBody EmbeddingRequest request) {
        float[][] embeddings = embeddingService.batchEmbed(request.getTexts());
        return ApiResponse.success(embeddings);
    }
}
