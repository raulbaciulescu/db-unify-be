package com.raulb.db_unify_be.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
public class ParsedQuery {
    private QueryType queryType;
    private Set<String> tables;
    private List<String> selectedColumns;
    private Expression whereCondition;
    private GroupByElement groupByColumns;
    private Expression havingCondition;
    private List<String> orderByColumns;
    private String originalSql;
    private List<Join> joins;
}
