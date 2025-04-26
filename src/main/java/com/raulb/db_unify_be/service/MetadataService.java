package com.raulb.db_unify_be.service;

import com.raulb.db_unify_be.entity.ColumnInfo;
import com.raulb.db_unify_be.entity.Connection;
import com.raulb.db_unify_be.entity.DatabaseType;
import com.raulb.db_unify_be.entity.TableInfo;
import com.raulb.db_unify_be.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetadataService {
    private final ConnectionRepository connectionRepository;
    private final DynamicDataSourceFactory factory;
    private final TypeConverterService typeConverterService;

    public List<TableInfo> getTablesAndColumns(Long connectionId) {
        Connection connection = getConnectionOrThrow(connectionId);
        DataSource dataSource = getOrCreateDataSource(connection);

        String query = getMetadataQuery(connection.getDatabaseType());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return groupColumnsByTable(rows);
    }

    private Connection getConnectionOrThrow(Long id) {
        return connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
    }

    private DataSource getOrCreateDataSource(Connection conn) {
        DataSource ds = factory.getCached(conn.getId());
        return (ds != null) ? ds : factory.createAndValidate(conn);
    }

    private String getMetadataQuery(DatabaseType dbType) {
        return switch (dbType) {
            case POSTGRES, SQLSERVER -> """
            SELECT table_name, column_name, data_type
            FROM information_schema.columns
            WHERE table_schema = 'public'
            ORDER BY table_name, ordinal_position
        """;
            case MYSQL -> String.format("""
            SELECT table_name, column_name, data_type
            FROM information_schema.columns
            WHERE table_schema = '%s'
            ORDER BY table_name, ordinal_position
        """, "example-mysql");
            case ORACLE -> """
            SELECT table_name, column_name, data_type
            FROM all_tab_columns
            WHERE owner = USER
            ORDER BY table_name, column_id
        """;
        };
    }

    private List<TableInfo> groupColumnsByTable(List<Map<String, Object>> rows) {
        Map<String, List<ColumnInfo>> grouped = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String tableName = String.valueOf(row.get("table_name"));
            String columnName = String.valueOf(row.get("column_name"));
            String dataType = String.valueOf(row.get("data_type"));
            String standardizedType = typeConverterService.mapToStandardType(dataType);

            grouped.computeIfAbsent(tableName, k -> new ArrayList<>())
                    .add(new ColumnInfo(columnName, standardizedType));
        }

        return grouped.entrySet().stream()
                .map(entry -> new TableInfo(entry.getKey(), entry.getValue()))
                .toList();
    }
}
