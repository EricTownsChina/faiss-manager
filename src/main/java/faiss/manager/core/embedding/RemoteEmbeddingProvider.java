package faiss.manager.core.embedding;

import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 远程 API Embedding 实现
 * <p>
 * 通过 HTTP 调用远程 Embedding 服务，支持批量请求。
 * </p>
 */
@Slf4j
public class RemoteEmbeddingProvider implements EmbeddingProvider {

    private final String url;
    private final int batchSize;
    private final int dimension;
    private final RestTemplate restTemplate;

    public RemoteEmbeddingProvider(String url, int batchSize, int dimension, RestTemplate restTemplate) {
        this.url = url;
        this.batchSize = batchSize;
        this.dimension = dimension;
        this.restTemplate = restTemplate;
    }

    @Override
    public float[] embed(String text) {
        float[][] result = batchEmbed(Collections.singletonList(text));
        return result[0];
    }

    @Override
    public float[][] batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new float[0][];
        }
        try {
            List<float[]> allEmbeddings = new ArrayList<>();
            // 分批调用
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);
                float[][] batchResult = callRemoteApi(batch);
                Collections.addAll(allEmbeddings, batchResult);
            }
            return allEmbeddings.toArray(new float[0][]);
        } catch (FaissManagerException e) {
            throw e;
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.EMBEDDING_FAILED,
                    "Embedding 生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    /**
     * 调用远程 Embedding API
     * <p>
     * 请求格式: {"texts": ["text1", "text2", ...]}
     * 响应格式: {"embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...], ...]}
     * </p>
     */
    @SuppressWarnings("unchecked")
    private float[][] callRemoteApi(List<String> texts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("texts", texts);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            EmbeddingResponse response = restTemplate.postForObject(url, request, EmbeddingResponse.class);
            if (response == null || response.getEmbeddings() == null) {
                throw new FaissManagerException(ErrorCode.EMBEDDING_FAILED, "远程 Embedding 返回空结果");
            }

            List<List<Double>> embeddings = response.getEmbeddings();
            float[][] result = new float[embeddings.size()][];
            for (int i = 0; i < embeddings.size(); i++) {
                List<Double> vec = embeddings.get(i);
                result[i] = new float[vec.size()];
                for (int j = 0; j < vec.size(); j++) {
                    result[i][j] = vec.get(j).floatValue();
                }
            }
            return result;
        } catch (FaissManagerException e) {
            throw e;
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.EMBEDDING_PROVIDER_UNAVAILABLE,
                    "远程 Embedding 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Data
    private static class EmbeddingResponse {
        private List<List<Double>> embeddings;
    }
}
