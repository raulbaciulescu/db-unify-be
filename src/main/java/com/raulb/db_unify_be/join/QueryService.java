package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.dtos.QueryResult;
import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.join.api.JoinAlgorithm;
import com.raulb.db_unify_be.service.DynamicDataSourceFactory;
import com.raulb.db_unify_be.service.DataFetcher;
import com.raulb.db_unify_be.service.GroupByService;
import com.raulb.db_unify_be.service.SqlParsingService;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.GroupByElement;
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
    private final GroupByService groupByService;

    private static final int DEFAULT_LIMIT = 5_000_000;

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

        if (parsedQuery.getGroupByColumns() != null) {
            return handleGroupBy(parsedQuery, rows);
        } else
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

        // Sortează join-urile în ordine crescătoare după dimensiuni implicite
        List<Join> optimizedJoins = reorderJoinsByEstimate(parsedQuery.getJoins(), tableEstimates);

        for (Join join : optimizedJoins) {
            System.out.println("Processing join: " + join);
            currentResult = performJoin(join, currentResult, parsedQuery, tableEstimates);
        }

        if (parsedQuery.getGroupByColumns() != null) {
            return handleGroupBy(parsedQuery, currentResult);
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
            long start = System.currentTimeMillis();
            leftRows = dataFetcher.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereCondition());
            System.out.println("Fetched left in " + (System.currentTimeMillis() - start) + "ms");
        }

        Connection rightConn = dataSourceFactory.getCachedByName(getSchemaName(rightTable));
        String rightTableName = getSimpleTableName(rightTable);
        long estimatedRightSize = estimates.getOrDefault(rightTable, 1_000_000L);
        JoinAlgorithm algorithm = joinStrategySelector.choose(leftRows.size(), estimatedRightSize);

        List<Map<String, Object>> result = new ArrayList<>();
        int pageOffset = 0;
        boolean done = false;

        while (!done) {
            long start = System.currentTimeMillis();
            List<Map<String, Object>> rightChunk = dataFetcher.selectChunkFromTableWithWhere(
                    rightConn.getId(), rightTableName, parsedQuery.getWhereCondition(), DEFAULT_LIMIT, pageOffset);
            System.out.println("Fetched right in " + (System.currentTimeMillis() - start) + "ms");
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

    public List<Map<String, String>> convertToListOfStringMaps(List<Map<String, Object>> rows) {
        List<Map<String, String>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, String> stringRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                stringRow.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
            }
            result.add(stringRow);
        }

        return result;
    }

    private List<Map<String, Object>> handleGroupBy(ParsedQuery parsedQuery, List<Map<String, Object>> rows) {
        List<Map<String, String>> convertedList = convertToListOfStringMaps(rows);
        GroupByElement groupByExpression = parsedQuery.getGroupByColumns();
        Map<List<String>, List<Map<String, String>>> rowsAfterGroupBy;

        if (groupByExpression != null) {
            List<String> groupByList = groupByExpression.getGroupByExpressionList().stream().map(Object::toString).toList();
            rowsAfterGroupBy = groupByService.doGroupBy(groupByList, convertedList);
            convertedList = groupByService.filterRowsGroupBy(parsedQuery, rowsAfterGroupBy, groupByList);
            return convertToListOfObjectMaps(convertedList);
            //Expression havingExpression = parsedQuery.getHavingCondition();
//            if (havingExpression != null) {
//                rows = groupByService.handleHaving(havingExpression, convertedList);
//            }
        } else
            return convertToListOfObjectMaps(convertedList);
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

    public List<Map<String, Object>> convertToListOfObjectMaps(List<Map<String, String>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, String> row : rows) {
            Map<String, Object> objectRow = new HashMap<>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (value == null) {
                    objectRow.put(key, null);
                } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    objectRow.put(key, Boolean.parseBoolean(value));
                } else {
                    try {
                        objectRow.put(key, Integer.parseInt(value));
                    } catch (NumberFormatException e1) {
                        try {
                            objectRow.put(key, Double.parseDouble(value));
                        } catch (NumberFormatException e2) {
                            objectRow.put(key, value);
                        }
                    }
                }
            }
            result.add(objectRow);
        }

        return result;
    }
}
