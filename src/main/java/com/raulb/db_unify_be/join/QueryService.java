package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.ParsedQuery;
import com.raulb.db_unify_be.join.api.JoinAlgorithm;
import com.raulb.db_unify_be.repository.ConnectionRepository;
import com.raulb.db_unify_be.service.DynamicDataSourceFactory;
import com.raulb.db_unify_be.service.SelectService;
import com.raulb.db_unify_be.service.SqlParsingService;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.SelectItem;
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


    public List<Map<String, Object>> execute(String sql) {
        ParsedQuery parsedQuery = sqlParsingService.parse(sql);

        if (parsedQuery.getJoins().isEmpty()) {
            return executeSingleTableSelect(parsedQuery);
        } else {
            return executeJoinQuery(parsedQuery);
        }
    }

    private List<Map<String, Object>> executeSingleTableSelect(ParsedQuery parsedQuery) {
        List<String> mainList = new ArrayList<>(parsedQuery.getTables());
        String fullTableName = mainList.get(0);

        Connection conn = dataSourceFactory.getCachedByName(getSchemaName(fullTableName));
        String tableName = getSimpleTableName(fullTableName);

        List<Map<String, Object>> rows = selectService.selectAllFromTable(conn.getId(), tableName);
        return filterSelectedColumns(rows, parsedQuery.getSelectedColumns());
    }

    private List<Map<String, Object>> executeJoinQuery(ParsedQuery parsedQuery) {
        Map<String, List<Map<String, Object>>> tableData = new HashMap<>();
        Map<String, Long> tableSizes = new HashMap<>();
        List<Map<String, Object>> current = null;

        for (Join join : parsedQuery.getJoins()) {
            current = performJoin(join, current, tableData, tableSizes);
        }

        return filterSelectedColumns(current, parsedQuery.getSelectedColumns());
    }

    private List<Map<String, Object>> performJoin(
            Join join,
            List<Map<String, Object>> current,
            Map<String, List<Map<String, Object>>> tableData,
            Map<String, Long> tableSizes) {

        Expression expression = join.getOnExpression();
        if (!(expression instanceof EqualsTo)) return current;

        EqualsTo equalsTo = (EqualsTo) expression;
        Column leftCol = (Column) equalsTo.getLeftExpression();
        Column rightCol = (Column) equalsTo.getRightExpression();

        String leftTable = getFullTableName(leftCol.getTable());
        String rightTable = getFullTableName(rightCol.getTable());
        String leftKey = leftCol.getColumnName();
        String rightKey = rightCol.getColumnName();

        List<Map<String, Object>> leftRows = current;
        if (!tableData.containsKey(leftTable)) {
            leftRows = loadTableIfNeeded(leftTable, tableData, tableSizes);
            if (current == null) current = leftRows;
        } else if (current == null) {
            leftRows = tableData.get(leftTable);
        }

        if (!tableData.containsKey(rightTable)) {
            loadTableIfNeeded(rightTable, tableData, tableSizes);
        }

        List<Map<String, Object>> rightRows = tableData.get(rightTable);
        long leftSize = (current == null) ? tableSizes.get(leftTable) : current.size();
        long rightSize = tableSizes.get(rightTable);

        JoinAlgorithm strategy = joinStrategySelector.choose(leftSize, rightSize);
        return strategy.join(leftRows, rightRows, leftKey, rightKey);
    }

    private List<Map<String, Object>> loadTableIfNeeded(
            String fullTableName,
            Map<String, List<Map<String, Object>>> tableData,
            Map<String, Long> tableSizes) {

        Connection conn = dataSourceFactory.getCachedByName(getSchemaName(fullTableName));
        String tableName = getSimpleTableName(fullTableName);

        List<Map<String, Object>> rows = selectService.selectAllFromTable(conn.getId(), tableName);
        tableData.put(fullTableName, rows);
        tableSizes.put(fullTableName, rowCountEstimator.estimateRowCount(conn, tableName).orElse((long) rows.size()));

        return rows;
    }

    private List<Map<String, Object>> filterSelectedColumns(List<Map<String, Object>> rows, List<String> selectedColumns) {
        if (selectedColumns == null || selectedColumns.isEmpty() || (selectedColumns.size() == 1 && selectedColumns.get(0).equals("*"))) {
            return rows; // SELECT *: return everything
        }

        return rows.stream()
                .map(row -> {
                    Map<String, Object> filtered = new HashMap<>();
                    for (String col : selectedColumns) {
                        if (row.containsKey(col)) {
                            filtered.put(col, row.get(col));
                        } else {
                            // Try last part in case it's a fully qualified name but row uses simple name
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
        String schema = table.getSchemaName(); // e.g., "population_db"
        String name = table.getName();         // e.g., "population"
        return (schema != null && !schema.isBlank()) ? schema + "." + name : name;
    }

    private String getSchemaName(String fullTableName) {
        int dotIndex = fullTableName.indexOf('.');
        return dotIndex > 0 ? fullTableName.substring(0, dotIndex) : fullTableName;
    }

    private String getSimpleTableName(String fullTableName) {
        int dotIndex = fullTableName.indexOf('.');
        return dotIndex > 0 ? fullTableName.substring(dotIndex + 1) : fullTableName;
    }
}
