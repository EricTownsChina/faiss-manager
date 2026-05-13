package faiss.manager.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 富化搜索结果（关联了元数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedSearchResult {

    private List<EnrichedMatch> matches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrichedMatch {

        /**
         * 向量 ID
         */
        private long vectorId;

        /**
         * 距离
         */
        private float distance;

        /**
         * 归一化相似度分数
         */
        private float score;

        /**
         * 业务文档 ID
         */
        private String documentId;

        /**
         * 原始文本
         */
        private String text;

        /**
         * 扩展业务数据
         */
        private Map<String, Object> extra;
    }
}
