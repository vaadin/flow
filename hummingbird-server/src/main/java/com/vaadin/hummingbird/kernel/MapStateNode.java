package com.vaadin.hummingbird.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class MapStateNode extends StateNode {
    private final Map<Object, Object> values = new HashMap<>();

    @Override
    public Object get(Object key) {
        return values.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    protected Object removeValue(Object key) {
        return values.remove(key);
    }

    @Override
    protected Object setValue(Object key, Object value) {
        return values.put(key, value);
    }

    @Override
    protected Stream<Object> getKeys() {
        return values.keySet().stream();
    }

    @Override
    public Class<?> getType(Object key) {
        return Object.class;
    }

}
