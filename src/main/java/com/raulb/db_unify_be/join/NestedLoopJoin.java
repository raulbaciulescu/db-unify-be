package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.service.api.JoinStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NestedLoopJoin implements JoinStrategy {

    @Override
    public String name() {
        return "Nested Loop Join";
    }

    @Override
    public List<Map<String, Object>> executeJoin(List<Map<String, Object>> leftTable, List<Map<String, Object>> rightTable, String leftKey, String rightKey) {
        System.out.println("Executing Nested Loop Join");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> leftRow : leftTable) {
            for (Map<String, Object> rightRow : rightTable) {
                if (Objects.equals(leftRow.get(leftKey), rightRow.get(rightKey))) {
                    Map<String, Object> merged = new HashMap<>();
                    merged.putAll(leftRow);
                    merged.putAll(rightRow);
                    result.add(merged);
                }
            }
        }
        return result;
    }
}

