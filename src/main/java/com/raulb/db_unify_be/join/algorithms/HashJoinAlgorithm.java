package com.raulb.db_unify_be.join.algorithms;

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
        Map<Object, List<Map<String, Object>>> hashMap = new HashMap<>();
        for (Map<String, Object> right : rightRows) {
            hashMap
                    .computeIfAbsent(right.get(rightKey), k -> new ArrayList<>())
                    .add(right);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> left : leftRows) {
            List<Map<String, Object>> matches = hashMap.getOrDefault(left.get(leftKey), List.of());
            for (Map<String, Object> match : matches) {
                Map<String, Object> joined = new HashMap<>(left);
                joined.putAll(match);
                result.add(joined);
            }
        }
        return result;
    }
}
