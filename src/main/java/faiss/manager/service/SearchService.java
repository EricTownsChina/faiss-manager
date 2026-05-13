package faiss.manager.service;

import faiss.manager.common.RateLimit;
import faiss.manager.core.engine.VectorEngine;
import faiss.manager.core.engine.model.SearchResult;
import faiss.manager.core.metadata.VectorMetadata;
import faiss.manager.core.metadata.VectorMetadataRepository;
import faiss.manager.service.dto.EnrichedSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量检索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final VectorEngine vectorEngine;
    private final EmbeddingService embeddingService;
    private final VectorMetadataRepository metadataRepository;

    /**
     * 向量直接查询
     */
    @RateLimit
    public EnrichedSearchResult searchByVector(String indexId, float[] vector, int topK) {
        SearchResult result = vectorEngine.search(indexId, vector, topK);
        return enrichResult(indexId, result);
    }

    /**
     * 文本语义查询：文本 → Embedding → 查询
     */
    @RateLimit
    public EnrichedSearchResult searchByText(String indexId, String text, int topK) {
        float[] vector = embeddingService.embed(text);
        SearchResult result = vectorEngine.search(indexId, vector, topK);
        return enrichResult(indexId, result);
    }

    /**
     * 批量向量查询
     */
    @RateLimit
    public List<EnrichedSearchResult> batchSearchByVector(String indexId, float[][] vectors, int topK) {
        List<SearchResult> results = vectorEngine.batchSearch(indexId, vectors, topK);
        return results.stream()
                .map(r -> enrichResult(indexId, r))
                .collect(Collectors.toList());
    }

    /**
     * 批量文本查询
     */
    @RateLimit
    public List<EnrichedSearchResult> batchSearchByText(String indexId, List<String> texts, int topK) {
        float[][] vectors = embeddingService.batchEmbed(texts);
        List<SearchResult> results = vectorEngine.batchSearch(indexId, vectors, topK);
        return results.stream()
                .map(r -> enrichResult(indexId, r))
                .collect(Collectors.toList());
    }

    /**
     * 将搜索结果关联元数据，生成富化结果
     */
    private EnrichedSearchResult enrichResult(String indexId, SearchResult result) {
        if (result.getMatches() == null || result.getMatches().isEmpty()) {
            return EnrichedSearchResult.builder().matches(new ArrayList<>()).build();
        }

        List<Long> vectorIds = result.getMatches().stream()
                .map(SearchResult.Match::getVectorId)
                .collect(Collectors.toList());

        // 批量查询元数据
        List<VectorMetadata> metadataList = metadataRepository.findByIndexIdAndVectorIdIn(indexId, vectorIds);
        Map<Long, VectorMetadata> metadataMap = metadataList.stream()
                .collect(Collectors.toMap(VectorMetadata::getVectorId, m -> m, (a, b) -> a));

        List<EnrichedSearchResult.EnrichedMatch> enrichedMatches = result.getMatches().stream()
                .map(match -> {
                    VectorMetadata meta = metadataMap.get(match.getVectorId());
                    return EnrichedSearchResult.EnrichedMatch.builder()
                            .vectorId(match.getVectorId())
                            .distance(match.getDistance())
                            .score(match.getScore())
                            .documentId(meta != null ? meta.getDocumentId() : null)
                            .text(meta != null ? meta.getText() : null)
                            .extra(meta != null ? meta.getExtra() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return EnrichedSearchResult.builder().matches(enrichedMatches).build();
    }
}
