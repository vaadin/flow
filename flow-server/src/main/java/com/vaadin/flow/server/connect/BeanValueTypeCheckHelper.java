package com.vaadin.flow.server.connect;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A helper class to break circular nullable checking.
 */
class BeanValueTypeCheckHelper {
    private Map<Type, Set<Object>> cache;

    boolean hasVisited(Object value, Type type) {
        if (cache == null) {
            return false;
        }
        Set<Object> values = cache.get(type);
        if (values == null) {
            return false;
        }
        return values.contains(value);
    }

    void markAsVisited(Object value, Type type) {
        if (cache == null) {
            cache = new HashMap<>();
        }
        cache.putIfAbsent(type, new HashSet<>());
        cache.get(type).add(value);
    }
}