package com.raulb.db_unify_be.dtos;

public record QueryRequest(String query, int offset) {
    public QueryRequest {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
    }
}
