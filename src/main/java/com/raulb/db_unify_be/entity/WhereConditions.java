package com.raulb.db_unify_be.entity;


import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class WhereConditions {
    private Map<String, String> equals = new LinkedHashMap<>();
    private Map<String, String> notEquals = new LinkedHashMap<>();
    private Map<String, String> greater = new LinkedHashMap<>();
    private Map<String, String> greaterEq = new LinkedHashMap<>();
    private Map<String, String> minor = new LinkedHashMap<>();
    private Map<String, String> minorEq = new LinkedHashMap<>();
}
