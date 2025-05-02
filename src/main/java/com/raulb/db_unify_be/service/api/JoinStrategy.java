package com.raulb.db_unify_be.service.api;

import java.util.List;
import java.util.Map;

public interface JoinStrategy {
    String name();

    List<Map<String, Object>> executeJoin(List<Map<String, Object>> leftTable, List<Map<String, Object>> rightTable, String leftKey, String rightKey);
}
