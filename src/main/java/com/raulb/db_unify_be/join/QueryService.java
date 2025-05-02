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

        Map<String, List<Map<String, Object>>> tableData = new HashMap<>();
        Map<String, Long> tableSizes = new HashMap<>();
        List<Map<String, Object>> current = null;

        for (Join join : parsedQuery.getJoins()) {
            Expression expression = join.getOnExpression();
            if (!(expression instanceof EqualsTo)) continue;
            EqualsTo equalsTo = (EqualsTo) expression;
            Expression leftExpr = equalsTo.getLeftExpression();
            Expression rightExpr = equalsTo.getRightExpression();

            String leftTable = getFullTableName(((Column) leftExpr).getTable());
            String rightTable = getFullTableName(((Column) rightExpr).getTable());

            String leftKey = ((Column) leftExpr).getColumnName();
            String rightKey = ((Column) rightExpr).getColumnName();  // <- should use rightExpr here

            // Load left table if needed
            List<Map<String, Object>> leftRows = current;
            if (!tableData.containsKey(leftTable)) {
                Connection conn = dataSourceFactory.getCachedByName(getSchemaName(leftTable));
                String tableName = getSimpleTableName(leftTable);

                List<Map<String, Object>> rows = selectService.selectAllFromTable(conn.getId(), tableName);
                tableData.put(leftTable, rows);
                tableSizes.put(leftTable, rowCountEstimator.estimateRowCount(conn, tableName).orElse((long) rows.size()));

                if (current == null) leftRows = rows;
            } else if (current == null) {
                leftRows = tableData.get(leftTable);
            }

            // Load right table if needed
            if (!tableData.containsKey(rightTable)) {
                Connection conn = dataSourceFactory.getCachedByName(getSchemaName(rightTable));
                String tableName = getSimpleTableName(rightTable);

                List<Map<String, Object>> rows = selectService.selectAllFromTable(conn.getId(), tableName);
                tableData.put(rightTable, rows);
                tableSizes.put(rightTable, rowCountEstimator.estimateRowCount(conn, tableName).orElse((long) rows.size()));
            }

            List<Map<String, Object>> rightRows = tableData.get(rightTable);
            long leftSize = (current == null) ? tableSizes.get(leftTable) : current.size();
            long rightSize = tableSizes.get(rightTable);

            JoinAlgorithm strategy = joinStrategySelector.choose(leftSize, rightSize);
            current = strategy.join(leftRows, rightRows, leftKey, rightKey);
        }

        // Filter to selected columns
        return filterSelectedColumns(current, parsedQuery.getSelectedColumns());
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
