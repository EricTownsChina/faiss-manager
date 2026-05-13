package faiss.manager.controller.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 文本搜索请求
 */
@Data
public class TextSearchRequest {

    @NotBlank(message = "text 不能为空")
    private String text;

    @Min(value = 1, message = "topK 必须大于 0")
    private int topK = 10;
}
