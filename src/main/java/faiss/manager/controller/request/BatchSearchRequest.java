package faiss.manager.controller.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量搜索请求
 */
@Data
public class BatchSearchRequest {

    /**
     * 查询向量列表
     */
    private List<float[]> vectors;

    /**
     * 查询文本列表（与 vectors 二选一）
     */
    private List<String> texts;

    @Min(value = 1, message = "topK 必须大于 0")
    private int topK = 10;
}
