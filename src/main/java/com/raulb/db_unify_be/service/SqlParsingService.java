package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.entity.QueryType;
import com.raulb.db_unify_be.exception.SqlParsingException;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SqlParsingService {
    public ParsedQuery parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            if (statement instanceof Select select) {
                return parseSelect(select, sql);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new SqlParsingException("Failed to parse SQL: " + e.getMessage(), e);
        }
    }

    private ParsedQuery parseSelect(Select select, String originalSql) throws JSQLParserException {
        Set<String> tables = new HashSet<>();
        List<String> columns = new ArrayList<>();
        GroupByElement groupByColumns = null;
        List<String> orderByColumns = new ArrayList<>();
        List<Join> joins = new ArrayList<>();
        Expression whereCondition = null;
        Expression havingCondition = null;

        Select selectBody = select.getSelectBody();

        if (selectBody instanceof PlainSelect plainSelect) {
            // Tables
            tables = TablesNamesFinder.findTables(originalSql);

            // Columns
            columns = plainSelect.getSelectItems().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

            // Joins
            if (plainSelect.getJoins() != null) {
                joins = plainSelect.getJoins();
            }

            // WHERE
            if (plainSelect.getWhere() != null) {
                whereCondition = plainSelect.getWhere();
            }

            if (plainSelect.getGroupBy() != null) {
                groupByColumns = plainSelect.getGroupBy();
            }

            // HAVING
            if (plainSelect.getHaving() != null) {
                havingCondition = plainSelect.getHaving();
            }
        }

        return ParsedQuery.builder()
                .queryType(QueryType.SELECT)
                .selectedColumns(columns)
                .joins(joins)
                .tables(tables)
                .groupByColumns(groupByColumns)
                .whereCondition(whereCondition)
                .havingCondition(havingCondition)
                .build();
    }
}
