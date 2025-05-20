package com.raulb.db_unify_be.join.api;

public interface SqlDialect {
    String applyPagination(String baseQuery, int limit, int offset);
}