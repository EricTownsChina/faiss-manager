package faiss.manager.core.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * 匹配列表，按相似度排序
     */
    private List<Match> matches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {

        /**
         * 向量 ID
         */
        private long vectorId;

        /**
         * 距离值（L2: 越小越相似; InnerProduct: 越大越相似）
         */
        private float distance;

        /**
         * 归一化相似度分数 [0, 1]，越大越相似
         */
        private float score;
    }
}
