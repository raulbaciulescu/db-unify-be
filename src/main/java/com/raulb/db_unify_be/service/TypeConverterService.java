package com.raulb.db_unify_be.service;

import org.springframework.stereotype.Service;

@Service
public class TypeConverterService {

    public String mapToStandardType(String nativeType) {
        if (nativeType == null) {
            return "UNKNOWN";
        }

        String normalized = nativeType.toUpperCase();

        // Handle typical types
        if (matches(normalized, "INT", "INTEGER", "SMALLINT", "BIGINT", "TINYINT", "NUMBER(10,0)")) {
            return "INTEGER";
        } else if (matches(normalized, "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE", "REAL", "NUMBER")) {
            return "DECIMAL";
        } else if (matches(normalized, "CHAR", "VARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2", "TEXT", "CLOB")) {
            return "VARCHAR";
        } else if (matches(normalized, "DATE", "DATETIME", "TIMESTAMP", "SMALLDATETIME", "TIME")) {
            return "TIMESTAMP";
        } else if (matches(normalized, "BLOB", "BINARY", "VARBINARY", "IMAGE")) {
            return "BINARY";
        } else if (matches(normalized, "BOOLEAN", "BIT")) {
            return "BOOLEAN";
        } else {
            return "OTHER"; // fallback
        }
    }

    private boolean matches(String normalizedType, String... candidates) {
        for (String candidate : candidates) {
            if (normalizedType.startsWith(candidate)) {
                return true;
            }
        }
        return false;
    }
}
