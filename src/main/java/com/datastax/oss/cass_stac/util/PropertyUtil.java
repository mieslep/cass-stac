package com.datastax.oss.cass_stac.util;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;



public class PropertyUtil {
    public static Map<String,Boolean> getBooleans(final Map<String, Object> properties) {
        final Map<String, Boolean> booleanMap = new HashMap<>();
        properties.entrySet().stream()
            .filter(e -> (e.getValue() instanceof Boolean))
            .forEach((e -> booleanMap.put(e.getKey(), (Boolean) e.getValue())));
        return booleanMap;
    }

    
    public static Map<String,String> getTexts(final Map<String, Object> properties) {
        final Map<String, String> textMap = new HashMap<>();
        properties.entrySet().stream()
            .filter(e -> (e.getValue() instanceof String))
            .forEach((e -> textMap.put(e.getKey(), (String) e.getValue())));
        return textMap;
    }

    public static Map<String,Double> getNumbers(final Map<String, Object> properties) {
        final Map<String, Double> numberMap = new HashMap<>();
        properties.entrySet().stream()
            .filter(e -> (e.getValue() instanceof Number))
            .forEach((e -> numberMap.put(e.getKey(), ((Number) e.getValue()).doubleValue())));
        return numberMap;
    }

    public static Map<String,OffsetDateTime> getDateTimes(final Map<String, Object> properties) {
        final Map<String, OffsetDateTime> datetimeMap = new HashMap<>();
        properties.entrySet().stream()
            .filter(e -> (e.getValue() instanceof Boolean))
            .forEach((e -> datetimeMap.put(e.getKey(), (OffsetDateTime) e.getValue())));
        return datetimeMap;
    }
}
