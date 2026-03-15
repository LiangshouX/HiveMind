package com.liangshou.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "Success"),
    SYSTEM_ERROR(500, "System Error"),
    PARAM_ERROR(400, "Parameter Error"),
    NOT_FOUND(404, "Not Found"),
    UNAUTHORIZED(401, "Unauthorized");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
