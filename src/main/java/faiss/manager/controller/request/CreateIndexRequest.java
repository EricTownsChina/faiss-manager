package faiss.manager.controller.request;

import faiss.manager.core.engine.model.MetricType;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 创建索引请求
 */
@Data
public class CreateIndexRequest {

    @NotBlank(message = "indexId 不能为空")
    private String indexId;

    @Min(value = 1, message = "dimension 必须大于 0")
    private int dimension;

    private MetricType metricType = MetricType.L2;

    private String indexType = "IDMap,Flat";

    private Map<String, Object> parameters;
}
