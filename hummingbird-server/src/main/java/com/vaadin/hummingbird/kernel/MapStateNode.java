package com.vaadin.hummingbird.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.vaadin.hummingbird.kernel.ValueType.ObjectType;

public class MapStateNode extends AbstractStateNode {
    private final Map<Object, Object> values = new HashMap<>();
    private final ObjectType type;

    public MapStateNode(ObjectType type) {
        this.type = type;

        // Init default values from the type
        type.getPropertyTypes().forEach((name, propertyType) -> {
            put(name, propertyType.getDefaultValue());
        });
    }

    @Override
    protected Object doGet(Object key) {
        return values.get(key);
    }

    @Override
    protected boolean doesContainKey(Object key) {
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
    protected Stream<Object> doGetKeys() {
        return values.keySet().stream();
    }

    @Override
    public Class<?> getType(Object key) {
        return Object.class;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + getId() + ", values="
                + values + "]";
    }

    @Override
    public ObjectType getType() {
        return type;
    }

}
