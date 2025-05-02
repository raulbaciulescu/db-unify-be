package com.raulb.db_unify_be.join.api;

import java.util.List;
import java.util.Map;

public interface JoinAlgorithm {
    List<Map<String, Object>> join(
            List<Map<String, Object>> leftRows,
            List<Map<String, Object>> rightRows,
            String leftKey,
            String rightKey
    );
}
