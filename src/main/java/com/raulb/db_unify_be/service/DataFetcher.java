package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.entity.CachedDataSource;
import com.raulb.db_unify_be.entity.DatabaseType;
import com.raulb.db_unify_be.join.SqlDialect;
import com.raulb.db_unify_be.join.SqlDialectFactory;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.raulb.db_unify_be.entity.CachedDataSource;
import com.raulb.db_unify_be.entity.DatabaseType;
import com.raulb.db_unify_be.entity.WhereConditions;
import com.raulb.db_unify_be.join.SqlDialect;
import com.raulb.db_unify_be.join.SqlDialectFactory;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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

    public List<Map<String, Object>> selectChunkFromTableWithWhere(
            Long connectionId,
            String tableName,
            Expression whereCondition,
            int limit, int offset) {
        CachedDataSource cached = validateAndGetDataSource(connectionId);
        DatabaseType dbType = cached.connection().getDatabaseType();
        SqlDialect dialect = SqlDialectFactory.getDialect(dbType);

        String baseQuery = "SELECT * FROM " + tableName;

        if (whereCondition == null) {
            return selectAllFromTable(connectionId, tableName);
        }

        String dbName = cached.connection().getName();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(cached.dataSource());

        WhereConditions conditions = new WhereConditions();
        parseWhereExpression(whereCondition, dbName, tableName, conditions);
        String query = buildSelectQueryWithConditionsWithChunks(baseQuery, tableName, conditions);
        query = dialect.applyPagination(query, limit, offset);

        System.out.println(query);
        return jdbcTemplate.queryForList(query);
    }

    public List<Map<String, Object>> selectAllFromTable(Long connectionId, String tableName) {
        DataSource dataSource = dataSourceFactory.getCached(connectionId);
        if (dataSource == null) {
            throw new IllegalArgumentException("No DataSource found for connection ID: " + connectionId);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        System.out.println("SELECT * FROM " + tableName);
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
    }

    public List<Map<String, Object>> selectFromTableWithWhere(
            Long connectionId,
            String tableName,
            Expression whereCondition) {
        if (whereCondition == null) {
            return selectAllFromTable(connectionId, tableName);
        }

        CachedDataSource cached = validateAndGetDataSource(connectionId);
        String dbName = cached.connection().getName();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(cached.dataSource());

        WhereConditions conditions = new WhereConditions();
        parseWhereExpression(whereCondition, dbName, tableName, conditions);
        String query = buildSelectQueryWithConditions(tableName, conditions);

        System.out.println(query);
        return jdbcTemplate.queryForList(query);
    }

    private CachedDataSource validateAndGetDataSource(Long connectionId) {
        CachedDataSource cached = dataSourceFactory.getCachedDataSource(connectionId);
        if (cached.dataSource() == null) {
            throw new IllegalArgumentException("No DataSource found for connection ID: " + connectionId);
        }
        return cached;
    }

    private String buildSelectQueryWithConditionsWithChunks(String queryWithChunks, String tableName, WhereConditions cond) {
        StringBuilder query = new StringBuilder(queryWithChunks);
        StringJoiner conditions = new StringJoiner(" AND ");

        cond.getEquals().forEach((col, val) -> conditions.add(col + " = " + val));
        cond.getNotEquals().forEach((col, val) -> conditions.add(col + " <> " + val));
        cond.getGreater().forEach((col, val) -> conditions.add(col + " > " + val));
        cond.getGreaterEq().forEach((col, val) -> conditions.add(col + " >= " + val));
        cond.getMinor().forEach((col, val) -> conditions.add(col + " < " + val));
        cond.getMinorEq().forEach((col, val) -> conditions.add(col + " <= " + val));

        if (conditions.length() > 0) {
            query.append(" WHERE ").append(conditions);
        }

        return query.toString();
    }

    private String buildSelectQueryWithConditions(String tableName, WhereConditions cond) {
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(tableName);
        StringJoiner conditions = new StringJoiner(" AND ");

        cond.getEquals().forEach((col, val) -> conditions.add(col + " = " + val));
        cond.getNotEquals().forEach((col, val) -> conditions.add(col + " <> " + val));
        cond.getGreater().forEach((col, val) -> conditions.add(col + " > " + val));
        cond.getGreaterEq().forEach((col, val) -> conditions.add(col + " >= " + val));
        cond.getMinor().forEach((col, val) -> conditions.add(col + " < " + val));
        cond.getMinorEq().forEach((col, val) -> conditions.add(col + " <= " + val));

        if (conditions.length() > 0) {
            query.append(" WHERE ").append(conditions);
        }

        return query.toString();
    }

    private void parseWhereExpression(Expression expr, String dbName, String tableName, WhereConditions conds) {
        if (expr instanceof AndExpression andExpr) {
            parseWhereExpression(andExpr.getLeftExpression(), dbName, tableName, conds);
            parseWhereExpression(andExpr.getRightExpression(), dbName, tableName, conds);
        } else if (expr instanceof EqualsTo e) {
            processBinaryExpression(e.getLeftExpression().toString(), e.getRightExpression().toString(), dbName, tableName, conds.getEquals());
        } else if (expr instanceof NotEqualsTo e) {
            processBinaryExpression(e.getLeftExpression().toString(), e.getRightExpression().toString(), dbName, tableName, conds.getNotEquals());
        } else if (expr instanceof GreaterThan e) {
            processBinaryExpression(e.getLeftExpression().toString(), e.getRightExpression().toString(), dbName, tableName, conds.getGreater());
        } else if (expr instanceof GreaterThanEquals e) {
            processBinaryExpression(e.getLeftExpression().toString(), e.getRightExpression().toString(), dbName, tableName, conds.getGreaterEq());
        } else if (expr instanceof MinorThan e) {
            processBinaryExpression(e.getLeftExpression().toString(), e.getRightExpression().toString(), dbName, tableName, conds.getMinor());
        } else if (expr instanceof MinorThanEquals e) {
            processBinaryExpression(e.getLeftExpression().toString(), e.getRightExpression().toString(), dbName, tableName, conds.getMinorEq());
        } else {
            System.out.println("Unhandled expression type: " + expr.getClass().getSimpleName());
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