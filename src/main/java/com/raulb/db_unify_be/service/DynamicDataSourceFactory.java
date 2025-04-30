package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.DatabaseType;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicDataSourceFactory {
    private final Map<Long, DataSource> cache = new ConcurrentHashMap<>();

    public DataSource createAndValidate(Connection conn) {
        try {
            String url = conn.getDatabaseType()
                    .getJdbcUrl(conn.getHost(), conn.getPort(), conn.getName());

            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName(conn.getDatabaseType().getDriver());
            ds.setUrl(url);

            // Handle authentication
            if (conn.getDatabaseType() != DatabaseType.SQLSERVER) {
                // Other databases (Postgres, MySQL, Oracle) require username and password
                ds.setUsername(conn.getUsername());
                ds.setPassword(conn.getPassword());
            }

            // Test connection
            try (java.sql.Connection testConn = ds.getConnection()) {
                cache.put(conn.getId(), ds);
                return ds;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to database: " + conn.getName(), e);
        }
    }

    public DataSource getCached(Long id) {
        return cache.get(id);
    }

    public void clearCache() {
        cache.clear();
    }
}
