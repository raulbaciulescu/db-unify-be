package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.join.api.SqlDialect;

public class PostgresSqlDialect implements SqlDialect {
    public String applyPagination(String baseQuery, int limit, int offset) {
        return baseQuery + String.format(" LIMIT %d OFFSET %d", limit, offset);
    }
}