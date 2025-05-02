package com.raulb.db_unify_be.join;

import com.raulb.db_unify_be.join.api.JoinAlgorithm;

import java.util.*;

public class SortMergeJoinAlgorithm implements JoinAlgorithm {
    @Override
    public List<Map<String, Object>> join(
            List<Map<String, Object>> leftRows,
            List<Map<String, Object>> rightRows,
            String leftKey,
            String rightKey
    ) {
        System.out.println("Sort Merge Join Algorithm");
        leftRows.sort(Comparator.comparing(row -> (Comparable) row.get(leftKey)));
        rightRows.sort(Comparator.comparing(row -> (Comparable) row.get(rightKey)));

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0, j = 0;

        while (i < leftRows.size() && j < rightRows.size()) {
            Comparable lVal = (Comparable) leftRows.get(i).get(leftKey);
            Comparable rVal = (Comparable) rightRows.get(j).get(rightKey);

            int cmp = lVal.compareTo(rVal);
            if (cmp == 0) {
                Map<String, Object> joined = new HashMap<>(leftRows.get(i));
                joined.putAll(rightRows.get(j));
                result.add(joined);

                int k = j + 1;
                while (k < rightRows.size() &&
                        Objects.equals(rightRows.get(k).get(rightKey), rVal)) {
                    Map<String, Object> nextJoin = new HashMap<>(leftRows.get(i));
                    nextJoin.putAll(rightRows.get(k));
                    result.add(nextJoin);
                    k++;
                }
                i++;
            } else if (cmp < 0) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }
}
