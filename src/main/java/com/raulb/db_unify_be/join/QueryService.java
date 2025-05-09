package com.raulb.db_unify_be.join;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final SqlParsingService sqlParsingService;
    private final RowCountEstimator rowCountEstimator;
    private final JoinStrategySelector joinStrategySelector;
    private final SelectService selectService;
    private final DynamicDataSourceFactory dataSourceFactory;
    //private final QueryValidator queryValidator;

    public List<Map<String, Object>> execute(String sql) {
        ParsedQuery parsedQuery = sqlParsingService.parse(sql);

//        if (!queryValidator.isValid(parsedQuery)) {
//            throw new IllegalArgumentException("Query failed validation: check table names and join compatibility.");
//        }

        if (parsedQuery.getJoins().isEmpty()) {
            return executeSingleTableSelect(parsedQuery);
        } else {
            return executeJoinQuery(parsedQuery);
        }
    }

    private List<Map<String, Object>> executeSingleTableSelect(ParsedQuery parsedQuery) {
        List<String> mainList = new ArrayList<>(parsedQuery.getTables());
        String fullTableName = mainList.get(0);
        String tableName = getSimpleTableName(fullTableName);
        String schema = getSchemaName(fullTableName);
        Connection conn = dataSourceFactory.getCachedByName(schema);

      //  List<Map<String, Object>> rows = selectService.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereExpression());
        List<Map<String, Object>> rows = selectService.selectAllFromTable(conn.getId(), tableName);
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

        for (Join join : parsedQuery.getJoins()) {
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
            leftRows = selectService.selectFromTableWithWhere(conn.getId(), tableName, parsedQuery.getWhereCondition());
        }

        Connection rightConn = dataSourceFactory.getCachedByName(getSchemaName(rightTable));
        String rightTableName = getSimpleTableName(rightTable);
        List<Map<String, Object>> rightRows = selectService.selectFromTableWithWhere(rightConn.getId(), rightTableName, parsedQuery.getWhereCondition());

        long leftSize = leftRows.size(); // already loaded
        long rightSize = estimates.get(rightTable);

        JoinAlgorithm algorithm = joinStrategySelector.choose(leftSize, rightSize);
        return algorithm.join(leftRows, rightRows, leftKey, rightKey);
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
}
