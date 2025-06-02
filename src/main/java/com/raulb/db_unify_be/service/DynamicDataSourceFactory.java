package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.entity.CachedDataSource;
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
    private final Map<Long, CachedDataSource> cache = new ConcurrentHashMap<>();

    public DataSource createAndValidate(Connection conn) {
        try {
            String url = conn.getDatabaseType()
                    .getJdbcUrl(conn.getHost(), conn.getPort(), conn.getName());

            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName(conn.getDatabaseType().getDriver());
            ds.setUrl(url);
            ds.setUsername(conn.getUsername());
            ds.setPassword(conn.getPassword());

            // Test connection
            try (java.sql.Connection testConn = ds.getConnection()) {
                cache.put(conn.getId(), new CachedDataSource(conn, ds));
                return ds;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to database: " + conn.getName(), e);
        }
    }

    public DataSource getCached(Long id) {
        CachedDataSource cached = cache.get(id);
        return cached != null ? cached.dataSource() : null;
    }

    public CachedDataSource getCachedDataSource(Long id) {
        return cache.get(id);
    }

    public void clearCache() {
        cache.clear();
    }

    public Connection getCachedByName(String dbName) {
        return cache.values().stream()
                .map(CachedDataSource::connection)
                .filter(conn -> conn.getName().equalsIgnoreCase(dbName))
                .findFirst()
                .orElse(null);
    }
}
