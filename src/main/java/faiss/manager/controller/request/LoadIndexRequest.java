package faiss.manager.controller.request;

import faiss.manager.core.engine.model.MetricType;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 加载索引请求
 */
@Data
public class LoadIndexRequest {

    @Min(value = 1, message = "dimension 必须大于 0")
    private int dimension;

    private MetricType metricType = MetricType.L2;

    private String indexType = "IDMap,Flat";
}
