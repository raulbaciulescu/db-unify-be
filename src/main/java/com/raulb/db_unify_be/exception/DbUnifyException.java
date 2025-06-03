package com.raulb.db_unify_be.exception;

import org.springframework.http.HttpStatus;

public abstract class DbUnifyException extends RuntimeException {
    public DbUnifyException(String message) {
        super(message);
    }

    public DbUnifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract HttpStatus getHttpStatus();
}

