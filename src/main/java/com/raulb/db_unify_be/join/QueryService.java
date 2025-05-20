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

    private static final int DEFAULT_LIMIT = 50_000;

    public QueryResult execute(String sql, int offset) {
        ParsedQuery parsedQuery = sqlParsingService.parse(sql);
        List<Map<String, Object>> rows;

        if (parsedQuery.getJoins().isEmpty()) {
            rows = executeSingleTableSelect(parsedQuery, offset);
            boolean isDone = rows.size() < DEFAULT_LIMIT;
            return new QueryResult(rows, offset + DEFAULT_LIMIT, isDone);
        } else {
            ResultMeta meta = new ResultMeta();
            List<Map<String, Object>> result = executeJoinQuery(parsedQuery, offset, meta);
            return new QueryResult(result, meta.nextOffset, meta.isDone);
        }
    }

    private List<Map<String, Object>> executeSingleTableSelect(ParsedQuery parsedQuery, int offset) {
        String fullTableName = parsedQuery.getTables().iterator().next();
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

    private List<Map<String, Object>> executeJoinQuery(ParsedQuery parsedQuery, int offset, ResultMeta meta) {
        List<Map<String, Object>> currentResult = null;

        Map<String, Long> tableEstimates = new HashMap<>();
        for (String table : parsedQuery.getTables()) {
            Connection conn = dataSourceFactory.getCachedByName(getSchemaName(table));
            String simpleTable = getSimpleTableName(table);
            long estimated = rowCountEstimator.estimateRowCount(conn, simpleTable).orElse(1_000_000L);
            tableEstimates.put(table, estimated);
        }

        boolean isFirstJoin = true;
        for (Join join : parsedQuery.getJoins()) {
            currentResult = performJoin(join, currentResult, parsedQuery, tableEstimates, offset, meta, isFirstJoin);
            if (!meta.isDone) {
                // stop here and wait for next chunk
                break;
            }
            isFirstJoin = false;
        }

        return filterSelectedColumns(currentResult, parsedQuery.getSelectedColumns());
    }

    private List<Map<String, Object>> performJoin(
            Join join,
            List<Map<String, Object>> currentResult,
            ParsedQuery parsedQuery,
            Map<String, Long> estimates,
            int offset,
            ResultMeta meta,
            boolean paginateRight) {

        EqualsTo eq = (EqualsTo) join.getOnExpression();
        Column leftCol = (Column) eq.getLeftExpression();
        Column rightCol = (Column) eq.getRightExpression();

        String leftTable = getFullTableName(leftCol.getTable());
        String rightTable = getFullTableName(rightCol.getTable());
        String leftKey = leftCol.getColumnName();
        String rightKey = rightCol.getColumnName();

        List<Map<String, Object>> leftRows = currentResult;
        if (leftRows == null) {
            Connection conn = dataSourceFactory.getCachedByName(getSchemaName(leftTable));
            String tableName = getSimpleTableName(leftTable);
            leftRows = selectService.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereCondition());
        }

        Connection rightConn = dataSourceFactory.getCachedByName(getSchemaName(rightTable));
        String rightTableName = getSimpleTableName(rightTable);
        long estimatedRightSize = estimates.getOrDefault(rightTable, 1_000_000L);

        JoinAlgorithm algorithm = joinStrategySelector.choose(leftRows.size(), estimatedRightSize);

        List<Map<String, Object>> rightChunk;
        if (paginateRight && estimatedRightSize > DEFAULT_LIMIT) {
            rightChunk = selectService.selectChunkFromTable(
                    rightConn.getId(), rightTableName, DEFAULT_LIMIT, offset);
            meta.nextOffset = offset + DEFAULT_LIMIT;
            meta.isDone = rightChunk.size() < DEFAULT_LIMIT;
        } else {
            rightChunk = selectService.selectFromTableWithWhere(
                    rightConn.getId(), rightTableName, parsedQuery.getWhereCondition());
            meta.nextOffset = offset;
            meta.isDone = true;
        }

        return algorithm.join(leftRows, rightChunk, leftKey, rightKey);
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

    private static class ResultMeta {
        int nextOffset;
        boolean isDone;
    }
}
