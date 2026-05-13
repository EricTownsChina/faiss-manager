package faiss.manager.common.exception;

import faiss.manager.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class FaissManagerException extends RuntimeException {

    private final ErrorCode errorCode;

    public FaissManagerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public FaissManagerException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FaissManagerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public FaissManagerException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
