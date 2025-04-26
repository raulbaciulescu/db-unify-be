package com.raulb.db_unify_be.service;


import com.raulb.db_unify_be.entity.Connection;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicDataSourceFactory {

    private final Map<Long, DataSource> cache = new ConcurrentHashMap<>();

    /**
     * Creates and caches a DataSource from the provided Connection object.
     * Validates the connection by attempting to open it.
     * Throws an exception if the connection is invalid.
     */
    public DataSource createAndValidate(Connection conn) {
        try {
            String url = conn.getDatabaseType().getJdbcUrl(
                    conn.getHost(), conn.getPort(), conn.getName());

            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName(conn.getDatabaseType().getDriver());
            ds.setUrl(url);
            ds.setUsername(conn.getUsername());
            ds.setPassword(conn.getPassword());

            // Test connection
            java.sql.Connection testConn = ds.getConnection();

            cache.put(conn.getId(), ds);
            return ds;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to database: " + conn.getName(), e);
        }
    }

    /**
     * Retrieves a cached DataSource for a given Connection ID.
     * Returns null if the DataSource is not cached.
     */
    public DataSource getCached(Long id) {
        return cache.get(id);
    }

    /**
     * Clears the entire connection cache.
     * Optional method you can use for maintenance/testing.
     */
    public void clearCache() {
        cache.clear();
    }
}
