package com.raulb.db_unify_be.dtos;

public record ConnectionResponse(
        Long id,
        String name,
        String host,
        int port,
        String username,
        String password,
        String databaseType,
        String status
) {
}
