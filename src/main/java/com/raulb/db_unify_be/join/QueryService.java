package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.dtos.QueryResult;
import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.join.api.JoinAlgorithm;
import com.raulb.db_unify_be.service.DynamicDataSourceFactory;
import com.raulb.db_unify_be.service.SelectService;
import com.raulb.db_unify_be.service.SqlParsingService;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QueryService {
    private final SqlParsingService sqlParsingService;
    private final RowCountEstimator rowCountEstimator;
    private final JoinStrategySelector joinStrategySelector;
    private final SelectService selectService;
    private final DynamicDataSourceFactory dataSourceFactory;

    private static final int DEFAULT_LIMIT = 1_000;

    public QueryResult execute(String sql, int offset) {
        ParsedQuery parsedQuery = sqlParsingService.parse(sql);
        List<Map<String, Object>> rows;

        if (parsedQuery.getJoins().isEmpty()) {
            rows = executeSingleTableSelect(parsedQuery, offset);
            boolean isDone = rows.size() < DEFAULT_LIMIT;
            return new QueryResult(rows, offset + DEFAULT_LIMIT, isDone);
        } else {
            JoinResult result = executeJoinQuery(parsedQuery, offset);
            return new QueryResult(result.rows(), result.nextOffset(), result.isDone());
        }
    }

    private List<Map<String, Object>> executeSingleTableSelect(ParsedQuery parsedQuery, int offset) {
        List<String> mainList = new ArrayList<>(parsedQuery.getTables());
        String fullTableName = mainList.get(0);
        String tableName = getSimpleTableName(fullTableName);
        String schema = getSchemaName(fullTableName);
        Connection conn = dataSourceFactory.getCachedByName(schema);
        long estimatedRows = rowCountEstimator.estimateRowCount(conn, tableName).orElse(1_000_000L);
        List<Map<String, Object>> rows;

        if (estimatedRows > DEFAULT_LIMIT) {
            rows = selectService.selectChunkFromTable(conn.getId(), tableName, DEFAULT_LIMIT, offset);
        } else {
            rows = selectService.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereCondition());
        }

        return filterSelectedColumns(rows, parsedQuery.getSelectedColumns());
    }

    private JoinResult executeJoinQuery(ParsedQuery parsedQuery, int offset) {
        // Estimate row counts for all tables involved in the query
        Map<String, Long> tableEstimates = new HashMap<>();
        for (String table : parsedQuery.getTables()) {
            Connection conn = dataSourceFactory.getCachedByName(getSchemaName(table));
            String simpleTable = getSimpleTableName(table);
            long estimated = rowCountEstimator.estimateRowCount(conn, simpleTable).orElse(1_000_000L);
            tableEstimates.put(table, estimated);
        }

        // get base table (first in FROM clause)
        String baseTable = parsedQuery.getTables().iterator().next();
        Connection baseConn = dataSourceFactory.getCachedByName(getSchemaName(baseTable));
        String baseTableName = getSimpleTableName(baseTable);
        long baseEstimate = tableEstimates.get(baseTable);

        List<Map<String, Object>> baseRows = (baseEstimate > DEFAULT_LIMIT)
                ? selectService.selectChunkFromTable(baseConn.getId(), baseTableName, DEFAULT_LIMIT, offset)
                : selectService.selectFromTableWithWhere(baseConn.getId(), baseTableName, parsedQuery.getWhereCondition());

        boolean isDone = baseRows.size() < DEFAULT_LIMIT;
        List<Map<String, Object>> result = baseRows;

        for (Join join : parsedQuery.getJoins()) {
            EqualsTo eq = (EqualsTo) join.getOnExpression();
            Column leftCol = (Column) eq.getLeftExpression();
            Column rightCol = (Column) eq.getRightExpression();

            String rightTable = getFullTableName(rightCol.getTable());
            String rightKey = rightCol.getColumnName();
            String leftKey = leftCol.getColumnName();

            Connection rightConn = dataSourceFactory.getCachedByName(getSchemaName(rightTable));
            String rightTableName = getSimpleTableName(rightTable);
            long estimatedRightSize = tableEstimates.getOrDefault(rightTable, 1_000_000L);

            JoinAlgorithm algorithm = joinStrategySelector.choose(result.size(), estimatedRightSize);

            List<Map<String, Object>> rightRows = selectService.selectFromTableWithWhere(
                    rightConn.getId(), rightTableName, parsedQuery.getWhereCondition());

            result = algorithm.join(result, rightRows, leftKey, rightKey);
        }

        int nextOffset = offset + DEFAULT_LIMIT;
        return new JoinResult(filterSelectedColumns(result, parsedQuery.getSelectedColumns()), nextOffset, isDone);
    }

    private List<Map<String, Object>> filterSelectedColumns(List<Map<String, Object>> rows, List<String> selectedColumns) {
        if (selectedColumns == null || selectedColumns.isEmpty() || (selectedColumns.size() == 1 && selectedColumns.get(0).equals("*"))) {
            return rows;
        }

        return rows.stream()
                .map(row -> {
                    Map<String, Object> filtered = new HashMap<>();
                    for (String col : selectedColumns) {
                        if (row.containsKey(col)) {
                            filtered.put(col, row.get(col));
                        } else {
                            String[] parts = col.split("\\.");
                            String shortCol = parts[parts.length - 1];
                            if (row.containsKey(shortCol)) {
                                filtered.put(shortCol, row.get(shortCol));
                            }
                        }
                    }
                    return filtered;
                })
                .toList();
    }

    private String getFullTableName(Table table) {
        String schema = table.getSchemaName();
        String name = table.getName();
        return (schema != null && !schema.isBlank()) ? schema + "." + name : name;
    }

    private String getSchemaName(String fullTableName) {
        int dot = fullTableName.indexOf('.');
        return dot > 0 ? fullTableName.substring(0, dot) : fullTableName;
    }

    private String getSimpleTableName(String fullTableName) {
        int dotIndex = fullTableName.indexOf('.');
        return dotIndex > 0 ? fullTableName.substring(dotIndex + 1) : fullTableName;
    }

    private record JoinResult(List<Map<String, Object>> rows, int nextOffset, boolean isDone) {}
}