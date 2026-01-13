package com.xy.lucky.crypto.exception;

/**
 * 签名验签异常
 */
public class SignatureException extends RuntimeException {

    /**
     * 错误码
     */
    private final SignatureErrorCode errorCode;

    public SignatureException(SignatureErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SignatureException(SignatureErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    public SignatureException(SignatureErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public SignatureErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 签名错误码枚举
     */
    public enum SignatureErrorCode {
        SIGN_MISSING("签名缺失"),
        SIGN_INVALID("签名无效"),
        TIMESTAMP_MISSING("时间戳缺失"),
        TIMESTAMP_EXPIRED("请求已过期"),
        NONCE_MISSING("随机数缺失"),
        NONCE_DUPLICATE("重复请求"),
        ALGORITHM_NOT_SUPPORTED("不支持的签名算法");

        private final String message;

        SignatureErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}

