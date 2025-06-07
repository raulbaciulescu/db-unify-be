package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.join.api.SqlDialect;

public class OracleSqlDialect implements SqlDialect {
    public String applyPagination(String baseQuery, int limit, int offset) {
        return """
            SELECT * FROM (
                SELECT inner_query.*, ROWNUM rnum
                FROM (%s) inner_query
                WHERE ROWNUM <= %d
            ) WHERE rnum > %d
        """.formatted(baseQuery, offset + limit, offset);
    }
}
