package faiss.manager.controller.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Embedding 请求
 */
@Data
public class EmbeddingRequest {

    @NotEmpty(message = "texts 不能为空")
    private List<String> texts;
}
