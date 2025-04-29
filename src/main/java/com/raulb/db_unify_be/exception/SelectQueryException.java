package com.raulb.db_unify_be.exception;


public class SelectQueryException extends RuntimeException {
    public SelectQueryException(String msg) {
        super(msg);
    }
}
