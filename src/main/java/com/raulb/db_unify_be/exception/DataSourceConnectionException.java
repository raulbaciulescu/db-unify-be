package com.raulb.db_unify_be.exception;

import org.springframework.http.HttpStatus;

public class DataSourceConnectionException extends DbUnifyException {
    public DataSourceConnectionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}