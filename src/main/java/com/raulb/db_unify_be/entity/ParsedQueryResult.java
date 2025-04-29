package com.raulb.db_unify_be.entity;

import net.sf.jsqlparser.statement.Statement;

public class ParsedQueryResult {
    private final QueryType queryType;
    private final Statement statement;

    public ParsedQueryResult(QueryType queryType, Statement statement) {
        this.queryType = queryType;
        this.statement = statement;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public Statement getStatement() {
        return statement;
    }
}