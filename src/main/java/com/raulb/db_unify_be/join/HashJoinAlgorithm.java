package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.join.api.JoinAlgorithm;

import java.util.*;

public class HashJoinAlgorithm implements JoinAlgorithm {
    @Override
    public List<Map<String, Object>> join(
            List<Map<String, Object>> leftRows,
            List<Map<String, Object>> rightRows,
            String leftKey,
            String rightKey
    ) {
        System.out.println("Hash Join Algorithm");

        // Extragem cheile scurte
        String shortLeftKey = getShortKey(leftKey);
        String shortRightKey = getShortKey(rightKey);

        // Construim indexul pentru dreapta
        Map<Object, List<Map<String, Object>>> rightIndex = new HashMap<>();
        for (Map<String, Object> rightRow : rightRows) {
            Object key = findValueByShortKey(rightRow, shortRightKey);
            if (key != null) {
                rightIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(rightRow);
            }
        }

        // Join efectiv
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> leftRow : leftRows) {
            Object key = findValueByShortKey(leftRow, shortLeftKey);
            if (key != null) {
                List<Map<String, Object>> matches = rightIndex.getOrDefault(key, List.of());
                for (Map<String, Object> match : matches) {
                    Map<String, Object> joined = new LinkedHashMap<>();
                    joined.putAll(leftRow);
                    joined.putAll(match);
                    result.add(joined);
                }
            }
        }

        return result;
    }

    private String getShortKey(String fullKey) {
        String[] parts = fullKey.split("\\.");
        return parts[parts.length - 1].toLowerCase();
    }

    private Object findValueByShortKey(Map<String, Object> row, String shortKey) {
        return row.entrySet().stream()
                .filter(e -> e.getKey().endsWith("." + shortKey) || e.getKey().equals(shortKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
