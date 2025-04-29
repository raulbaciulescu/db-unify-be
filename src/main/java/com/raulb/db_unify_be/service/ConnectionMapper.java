package com.raulb.db_unify_be.service;


import com.raulb.db_unify_be.dtos.ConnectionResponse;
import com.raulb.db_unify_be.entity.Connection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConnectionMapper {
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
