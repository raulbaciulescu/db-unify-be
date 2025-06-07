package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.join.api.JoinAlgorithm;
import com.raulb.db_unify_be.service.api.JoinStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NestedLoopJoin implements JoinAlgorithm {
    @Override
    public List<Map<String, Object>> join(
            List<Map<String, Object>> leftRows,
            List<Map<String, Object>> rightRows,
            String leftKey,
            String rightKey
    ) {
        System.out.println("Nested Loop Join Algorithm");

        String shortLeftKey = getShortKey(leftKey);
        String shortRightKey = getShortKey(rightKey);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> left : leftRows) {
            Object leftValue = findValueByShortKey(left, shortLeftKey);

            for (Map<String, Object> right : rightRows) {
                Object rightValue = findValueByShortKey(right, shortRightKey);

                if (Objects.equals(leftValue, rightValue)) {
                    Map<String, Object> joined = new LinkedHashMap<>();
                    joined.putAll(left);
                    joined.putAll(right);
                    result.add(joined);
                }
            }
        }

        return result;
    }

    private String getShortKey(String fullKey) {
        String[] parts = fullKey.split("\\.");
        return parts[parts.length - 1];
    }

    private Object findValueByShortKey(Map<String, Object> row, String shortKey) {
        return row.entrySet().stream()
                .filter(e -> e.getKey().endsWith("." + shortKey) || e.getKey().equals(shortKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}