package com.raulb.db_unify_be.entity;

import javax.sql.DataSource;

public record CachedDataSource(Connection connection, DataSource dataSource) {}
