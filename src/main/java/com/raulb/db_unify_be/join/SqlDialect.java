package com.raulb.db_unify_be.join;

public interface SqlDialect {
    String applyPagination(String baseQuery, int limit, int offset);
}