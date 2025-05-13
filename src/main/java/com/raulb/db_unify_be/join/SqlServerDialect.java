package com.raulb.db_unify_be.join;

public class SqlServerDialect implements SqlDialect {
    public String applyPagination(String baseQuery, int limit, int offset) {
        String orderBy = "ORDER BY (SELECT NULL)"; // fallback if no order is known
        if (!baseQuery.toLowerCase().contains("order by")) {
            return baseQuery + " " + orderBy + String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
        } else {
            return baseQuery + String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
        }
    }
}
