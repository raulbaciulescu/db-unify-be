package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.dtos.QueryResult;
import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.join.api.JoinAlgorithm;
import com.raulb.db_unify_be.service.DynamicDataSourceFactory;
import com.raulb.db_unify_be.service.DataFetcher;
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
    private final DataFetcher dataFetcher;
    private final DynamicDataSourceFactory dataSourceFactory;

    private static final int DEFAULT_LIMIT = 50_000;

    public QueryResult execute(String sql, int offset) {
        ParsedQuery parsedQuery = sqlParsingService.parse(sql);
        List<Map<String, Object>> rows;

        if (parsedQuery.getJoins().isEmpty()) {
            rows = executeSingleTableSelect(parsedQuery);
            boolean isDone = rows.size() < DEFAULT_LIMIT;
            return new QueryResult(rows, offset + DEFAULT_LIMIT, true);
        } else {
            List<Map<String, Object>> result = executeJoinQuery(parsedQuery);
            return new QueryResult(result, 1000, true);
        }
    }

    private List<Join> reorderJoinsByEstimate(List<Join> joins, Map<String, Long> estimates) {
        return joins.stream()
                .sorted(Comparator.comparingLong(j -> {
                    EqualsTo eq = (EqualsTo) j.getOnExpression();
                    Column left = (Column) eq.getLeftExpression();
                    Column right = (Column) eq.getRightExpression();
                    String leftTable = getFullTableName(left.getTable());
                    String rightTable = getFullTableName(right.getTable());
                    long leftSize = estimates.getOrDefault(leftTable, 1_000_000L);
                    long rightSize = estimates.getOrDefault(rightTable, 1_000_000L);
                    return leftSize + rightSize;
                }))
                .toList();
    }

    private List<Map<String, Object>> executeSingleTableSelect(ParsedQuery parsedQuery) {
        String fullTableName = parsedQuery.getTables().iterator().next();
        String tableName = getSimpleTableName(fullTableName);
        String schema = getSchemaName(fullTableName);
        Connection conn = dataSourceFactory.getCachedByName(schema);
        List<Map<String, Object>> rows;

        rows = dataFetcher.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereCondition());
        return filterSelectedColumns(rows, parsedQuery.getSelectedColumns());
    }

    private List<Map<String, Object>> executeJoinQuery(ParsedQuery parsedQuery) {
        List<Map<String, Object>> currentResult = null;

        Map<String, Long> tableEstimates = new HashMap<>();
        for (String table : parsedQuery.getTables()) {
            Connection conn = dataSourceFactory.getCachedByName(getSchemaName(table));
            String simpleTable = getSimpleTableName(table);
            long estimated = rowCountEstimator.estimateRowCount(conn, simpleTable).orElse(1_000_000L);
            tableEstimates.put(table, estimated);
        }

        // 🧠 Sortează join-urile în ordine crescătoare după dimensiuni implicite
        List<Join> optimizedJoins = reorderJoinsByEstimate(parsedQuery.getJoins(), tableEstimates);

        for (Join join : optimizedJoins) {
            System.out.println("Processing join: " + join);
            currentResult = performJoin(join, currentResult, parsedQuery, tableEstimates);
        }

        return filterSelectedColumns(currentResult, parsedQuery.getSelectedColumns());
    }

    private List<Map<String, Object>> performJoin(
            Join join,
            List<Map<String, Object>> currentResult,
            ParsedQuery parsedQuery,
            Map<String, Long> estimates) {

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
            leftRows = dataFetcher.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereCondition());
        }

        Connection rightConn = dataSourceFactory.getCachedByName(getSchemaName(rightTable));
        String rightTableName = getSimpleTableName(rightTable);
        long estimatedRightSize = estimates.getOrDefault(rightTable, 1_000_000L);
        JoinAlgorithm algorithm = joinStrategySelector.choose(leftRows.size(), estimatedRightSize);

        List<Map<String, Object>> result = new ArrayList<>();
        int pageOffset = 0;
        boolean done = false;

        while (!done) {
            List<Map<String, Object>> rightChunk = dataFetcher.selectChunkFromTableWithWhere(
                    rightConn.getId(), rightTableName, parsedQuery.getWhereCondition(), DEFAULT_LIMIT, pageOffset);
//            List<Map<String, Object>> rightChunk = dataFetcher.selectFromTableWithWhere(
//                    rightConn.getId(), rightTableName, parsedQuery.getWhereCondition());
            if (rightChunk.isEmpty()) {
                break;
            }

            List<Map<String, Object>> partialJoin = algorithm.join(leftRows, rightChunk, leftKey, rightKey);
            result.addAll(partialJoin);

            if (rightChunk.size() < DEFAULT_LIMIT) {
                done = true;
            } else {
                pageOffset += DEFAULT_LIMIT;
            }
        }

        return result;
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
