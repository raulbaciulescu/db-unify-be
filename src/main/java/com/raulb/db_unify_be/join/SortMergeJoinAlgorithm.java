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

        String shortLeftKey = getShortKey(leftKey);
        String shortRightKey = getShortKey(rightKey);

        // Sortăm ambele liste după cheia scurtă
        Comparator<Map<String, Object>> comparator = Comparator.comparing(
                row -> String.valueOf(findValueByShortKey(row, shortLeftKey))
        );

        List<Map<String, Object>> sortedLeft = new ArrayList<>(leftRows);
        List<Map<String, Object>> sortedRight = new ArrayList<>(rightRows);

        sortedLeft.sort(comparator);
        sortedRight.sort(comparator);

        List<Map<String, Object>> result = new ArrayList<>();

        int i = 0, j = 0;
        while (i < sortedLeft.size() && j < sortedRight.size()) {
            Object leftValue = findValueByShortKey(sortedLeft.get(i), shortLeftKey);
            Object rightValue = findValueByShortKey(sortedRight.get(j), shortRightKey);

            int cmp = compare(leftValue, rightValue);
            if (cmp == 0) {
                // Găsim toate potrivirile (pot fi multiple în dreapta)
                int tempJ = j;
                while (tempJ < sortedRight.size() &&
                        Objects.equals(findValueByShortKey(sortedRight.get(tempJ), shortRightKey), leftValue)) {
                    Map<String, Object> joined = new LinkedHashMap<>();
                    joined.putAll(sortedLeft.get(i));
                    joined.putAll(sortedRight.get(tempJ));
                    result.add(joined);
                    tempJ++;
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

    private int compare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        return a.toString().compareTo(b.toString());
    }
}
