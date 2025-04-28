package com.raulb.db_unify_be.service;


import com.raulb.db_unify_be.dtos.ConnectionResponse;
import com.raulb.db_unify_be.entity.Connection;

public class ConnectionMapper {

    private ConnectionMapper() {
        // Private constructor to prevent instantiation
    }

    public static ConnectionResponse toResponse(Connection connection, String status) {
        if (connection == null) {
            return null;
        }

        return new ConnectionResponse(
                connection.getId(),
                connection.getName(),
                connection.getHost(),
                connection.getPort(),
                connection.getUsername(),
                connection.getPassword(),
                connection.getDatabaseType().name(),
                status
        );
    }
}
