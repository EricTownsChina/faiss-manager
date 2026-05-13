package faiss.manager.controller.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 向量搜索请求
 */
@Data
public class VectorSearchRequest {

    @NotNull(message = "vector 不能为空")
    private float[] vector;

    @Min(value = 1, message = "topK 必须大于 0")
    private int topK = 10;
}
