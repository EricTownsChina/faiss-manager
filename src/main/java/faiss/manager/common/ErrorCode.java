package faiss.manager.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    SUCCESS(0, "success"),
    INTERNAL_ERROR(10000, "内部错误"),
    PARAM_INVALID(10001, "参数错误"),
    RATE_LIMITED(10002, "请求过于频繁，请稍后重试"),

    // 索引相关 2xxxx
    INDEX_NOT_FOUND(20001, "索引不存在"),
    INDEX_ALREADY_EXISTS(20002, "索引已存在"),
    INDEX_NOT_LOADED(20003, "索引未加载到内存"),
    INDEX_ALREADY_LOADED(20004, "索引已加载"),
    INDEX_LOAD_FAILED(20005, "索引加载失败"),
    INDEX_SAVE_FAILED(20006, "索引保存失败"),
    INDEX_CREATE_FAILED(20007, "索引创建失败"),

    // 向量相关 3xxxx
    VECTOR_ADD_FAILED(30001, "向量添加失败"),
    VECTOR_REMOVE_FAILED(30002, "向量删除失败"),
    VECTOR_SEARCH_FAILED(30003, "向量查询失败"),
    VECTOR_DIMENSION_MISMATCH(30004, "向量维度不匹配"),

    // Embedding 相关 4xxxx
    EMBEDDING_FAILED(40001, "Embedding 生成失败"),
    EMBEDDING_PROVIDER_UNAVAILABLE(40002, "Embedding 服务不可用"),

    // 存储相关 5xxxx
    STORAGE_READ_FAILED(50001, "存储读取失败"),
    STORAGE_WRITE_FAILED(50002, "存储写入失败"),
    STORAGE_DELETE_FAILED(50003, "存储删除失败"),

    // 文档相关 6xxxx
    DOCUMENT_NOT_FOUND(60001, "文档不存在"),
    ;

    private final int code;
    private final String message;
}
