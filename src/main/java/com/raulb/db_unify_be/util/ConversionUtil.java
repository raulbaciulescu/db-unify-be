package com.raulb.db_unify_be.util;

import java.util.Map;
import java.util.Objects;

public class ConversionUtil {
    public static boolean canConvertToInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean areMapsEqual(Map<String, String> map1, Map<String, String> map2) {
        if (map1 == null && map2 == null) {
            return true;
        }

        if (map1 == null || map2 == null || map1.size() != map2.size()) {
            return false;
        }

        for (Map.Entry<String, String> entry : map1.entrySet()) {
            String key = entry.getKey();
            Object value1 = entry.getValue();
            Object value2 = map2.get(key);

            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }

        return true;
    }
}
