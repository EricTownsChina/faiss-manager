package faiss.manager.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 向量条目（已有向量，跳过 Embedding）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorEntry {

    /**
     * 业务文档 ID
     */
    @NotBlank(message = "documentId 不能为空")
    private String documentId;

    /**
     * 已生成的向量
     */
    @NotNull(message = "vector 不能为空")
    private float[] vector;

    /**
     * 原始文本（可选，用于元数据记录）
     */
    private String text;

    /**
     * 扩展元数据
     */
    private Map<String, Object> extra;
}
