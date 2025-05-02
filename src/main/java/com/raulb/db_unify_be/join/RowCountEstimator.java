package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.DatabaseType;
import com.raulb.db_unify_be.service.DynamicDataSourceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RowCountEstimator {
    private final DynamicDataSourceFactory dataSourceFactory;

    public Optional<Long> estimateRowCount(Connection conn, String tableName) {
        DataSource ds = dataSourceFactory.getCached(conn.getId());
        if (ds == null) {
            throw new IllegalStateException("No DataSource cached for ID: " + conn.getId());
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        DatabaseType dbType = conn.getDatabaseType();
        String sql = getQueryByDbType(dbType);

        try {
            return Optional.of(switch (dbType) {
                case POSTGRES, SQLSERVER -> jdbcTemplate.queryForObject(sql, Long.class, tableName);
                case MYSQL -> jdbcTemplate.queryForObject(sql, Long.class, conn.getName(), tableName);
                case ORACLE -> jdbcTemplate.queryForObject(sql, Long.class, conn.getUsername(), tableName);
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String getQueryByDbType(DatabaseType dbType) {
        return switch (dbType) {
            case POSTGRES -> """
                SELECT reltuples::BIGINT AS estimate
                FROM pg_class
                WHERE oid = ?::regclass
                """;
            case MYSQL -> """
                SELECT TABLE_ROWS
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                """;
            case ORACLE -> """
                SELECT NUM_ROWS
                FROM ALL_TABLES
                WHERE OWNER = UPPER(?) AND TABLE_NAME = UPPER(?)
                """;
            case SQLSERVER -> """
                SELECT SUM(p.rows) AS estimate
                FROM sys.partitions p
                JOIN sys.tables t ON p.object_id = t.object_id
                WHERE t.name = ?
                """;
        };
    }
}
