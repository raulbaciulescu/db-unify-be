package com.raulb.db_unify_be.dtos;

import java.util.List;
import java.util.Map;

public record QueryResult(List<Map<String, Object>> results, int offset, boolean isDone) {
    public QueryResult(List<Map<String, Object>> results) {
        this(results, 0, true);
    }

    public QueryResult(List<Map<String, Object>> results, int offset) {
        this(results, offset, true);
    }
}
