package com.raulb.db_unify_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SelectService {
    private final DynamicDataSourceFactory dataSourceFactory;

    public List<Map<String, Object>> selectAllFromTable(Long connectionId, String tableName) {
        DataSource ds = dataSourceFactory.getCached(connectionId);
        if (ds == null) {
            throw new IllegalArgumentException("No DataSource found for connection ID: " + connectionId);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        String query = "SELECT * FROM " + tableName; // ⚠️ Consider SQL injection risks for dynamic table names
        return jdbcTemplate.queryForList(query);
    }
}
