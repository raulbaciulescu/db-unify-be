package com.raulb.db_unify_be.entity;

public enum DatabaseType {
    POSTGRES, MYSQL, ORACLE, SQLSERVER;

    public String getJdbcUrl(String host, int port, String dbName) {
        return switch (this) {
            case POSTGRES -> "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + dbName;
            case ORACLE -> "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName;
            case SQLSERVER ->
                    "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + dbName + ";integratedSecurity=true;trustServerCertificate=true";
        };
    }

    public String getDriver() {
        return switch (this) {
            case POSTGRES -> "org.postgresql.Driver";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case ORACLE -> "oracle.jdbc.OracleDriver";
            case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        };
    }
}