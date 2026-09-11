package com.nursery.config;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * 业务异常，携带 HTTP 状态码，由全局异常处理器转换为 {message} 响应
 */
@Getter
public class BizException extends RuntimeException {
    private final HttpStatus status;

    public BizException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BizException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BizException notFound(String message) {
        return new BizException(HttpStatus.NOT_FOUND, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(HttpStatus.FORBIDDEN, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(HttpStatus.UNAUTHORIZED, message);
    }
}
