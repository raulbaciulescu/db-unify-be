package com.raulb.db_unify_be.exception;

import org.springframework.http.HttpStatus;

public class SqlParsingException extends DbUnifyException {
    public SqlParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}