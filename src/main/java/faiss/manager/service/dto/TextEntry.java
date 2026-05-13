package faiss.manager.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 文本条目（用于添加文本并自动 Embedding）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextEntry {

    /**
     * 业务文档 ID
     */
    @NotBlank(message = "documentId 不能为空")
    private String documentId;

    /**
     * 要向量化的文本
     */
    @NotBlank(message = "text 不能为空")
    private String text;

    /**
     * 扩展元数据
     */
    private Map<String, Object> extra;
}
