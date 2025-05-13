package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.entity.DatabaseType;

import static com.raulb.db_unify_be.entity.DatabaseType.MYSQL;
import static com.raulb.db_unify_be.entity.DatabaseType.POSTGRES;

public class SqlDialectFactory {
    public static SqlDialect getDialect(DatabaseType dbType) {
        return switch (dbType) {
            case POSTGRES, MYSQL -> new PostgresSqlDialect();
            case SQLSERVER -> new SqlServerDialect();
            case ORACLE -> new OracleSqlDialect();
        };
    }
}