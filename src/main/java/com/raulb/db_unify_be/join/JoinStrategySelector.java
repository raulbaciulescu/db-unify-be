package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.join.api.JoinAlgorithm;
import org.springframework.stereotype.Component;

@Component
public class JoinStrategySelector {
    public JoinAlgorithm choose(long leftSize, long rightSize) {
        if (leftSize < 10_000 && rightSize < 10_000) {
            return new SortMergeJoinAlgorithm();
        } else if (leftSize < 1_000_000 && rightSize < 1_000_000) {
            return new HashJoinAlgorithm();
        } else if (leftSize < 10_000_000 && rightSize < 10_000_000) {
            return new HashJoinAlgorithm(); // 👈 înlocuit
        } else {
            return new SortMergeJoinAlgorithm();
        }
    }
}
