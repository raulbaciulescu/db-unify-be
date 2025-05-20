package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.entity.CachedDataSource;
import com.raulb.db_unify_be.entity.DatabaseType;
import com.raulb.db_unify_be.join.api.SqlDialect;
import com.raulb.db_unify_be.join.SqlDialectFactory;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class DataFetcher {
    private final DynamicDataSourceFactory dataSourceFactory;

    public List<Map<String, Object>> selectChunkFromTable(Long connectionId, String tableName, int limit, int offset) {
        CachedDataSource cached = validateAndGetDataSource(connectionId);
        DatabaseType dbType = cached.connection().getDatabaseType();
        SqlDialect dialect = SqlDialectFactory.getDialect(dbType);

        String baseQuery = "SELECT * FROM " + tableName;
        String paginatedQuery = dialect.applyPagination(baseQuery, limit, offset);

        return new JdbcTemplate(cached.dataSource()).queryForList(paginatedQuery);
    }

    public List<Map<String, Object>> selectAllFromTable(Long connectionId, String tableName) {
        DataSource dataSource = dataSourceFactory.getCached(connectionId);
        if (dataSource == null) {
            throw new IllegalArgumentException("No DataSource found for connection ID: " + connectionId);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String query = String.format("SELECT * FROM %s", tableName);
        return jdbcTemplate.queryForList(query);
    }

    public List<Map<String, Object>> selectFromTableWithWhere(
            Long connectionId,
            String tableName,
            Expression whereCondition) {

        if (whereCondition == null) {
            return selectAllFromTable(connectionId, tableName);
        }

        CachedDataSource cachedDataSource = validateAndGetDataSource(connectionId);
        String dbName = cachedDataSource.connection().getName();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(cachedDataSource.dataSource());

        Map<String, String> equalsMap = new LinkedHashMap<>();
        Map<String, String> greaterMap = new LinkedHashMap<>();

        parseWhereExpression(whereCondition, dbName, tableName, equalsMap, greaterMap);
        String query = buildSelectQueryWithConditions(tableName, equalsMap, greaterMap);

        return jdbcTemplate.queryForList(query);
    }

    private CachedDataSource validateAndGetDataSource(Long connectionId) {
        CachedDataSource cachedDataSource = dataSourceFactory.getCachedDataSource(connectionId);
        if (cachedDataSource.dataSource() == null) {
            throw new IllegalArgumentException("No DataSource found for connection ID: " + connectionId);
        }
        return cachedDataSource;
    }

    private String buildSelectQueryWithConditions(String tableName, Map<String, String> equalsMap, Map<String, String> greaterMap) {
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(tableName);
        StringJoiner conditions = new StringJoiner(" AND ");

        equalsMap.forEach((column, value) -> conditions.add(column + " = " + value));
        greaterMap.forEach((column, value) -> conditions.add(column + " > " + value));

        if (conditions.length() > 0) {
            query.append(" WHERE ").append(conditions);
        }

        return query.toString();
    }

    private void parseWhereExpression(Expression expression, String dbName, String tableName,
                                      Map<String, String> equalsMap, Map<String, String> greaterMap) {
        if (expression instanceof AndExpression andExpr) {
            parseWhereExpression(andExpr.getLeftExpression(), dbName, tableName, equalsMap, greaterMap);
            parseWhereExpression(andExpr.getRightExpression(), dbName, tableName, equalsMap, greaterMap);
        } else if (expression instanceof EqualsTo equalsTo) {
            processBinaryExpression(equalsTo.getLeftExpression().toString(),
                    equalsTo.getRightExpression().toString(),
                    dbName, tableName, equalsMap);
        } else if (expression instanceof GreaterThan greaterThan) {
            processBinaryExpression(greaterThan.getLeftExpression().toString(),
                    greaterThan.getRightExpression().toString(),
                    dbName, tableName, greaterMap);
        } else {
            System.out.println("Unhandled expression type: " + expression.getClass().getSimpleName());
        }
    }

    private void processBinaryExpression(String leftExpr, String rightExpr, String dbName, String tableName,
                                         Map<String, String> targetMap) {
        String[] parts = leftExpr.split("\\.");
        if (parts.length != 3) {
            System.out.println("Invalid column reference: " + leftExpr);
            return;
        }

        String exprDbName = parts[0];
        String exprTableName = parts[1];
        String columnName = parts[2];

        if (dbName.equals(exprDbName) && tableName.equals(exprTableName)) {
            targetMap.put(columnName, rightExpr);
        }
    }
}
