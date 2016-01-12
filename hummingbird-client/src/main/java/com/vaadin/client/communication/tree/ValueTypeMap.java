package com.vaadin.client.communication.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValueTypeMap {
    private final Map<Integer, ValueType> valueTypes = new HashMap<>();

    public static final int STRING = 0;
    public static final int BOOLEAN = 1;
    public static final int BOOLEAN_PRIMITIVE = 2;
    public static final int INTEGER = 3;
    public static final int INTEGER_PRIMITIVE = 4;
    public static final int NUMBER = 5;
    public static final int NUMBER_PRIMITIVE = 6;
    public static final int EMPTY_OBJECT = 7;
    public static final int UNDEFINED = 8;
    public static final int UNDEFINED_ARRAY = 9;

    public ValueTypeMap() {
        register(new ValueType(STRING, null));
        register(new ValueType(BOOLEAN, null));
        register(new ValueType(BOOLEAN_PRIMITIVE, Boolean.FALSE));
        register(new ValueType(INTEGER, null));
        register(new ValueType(INTEGER_PRIMITIVE, Integer.valueOf(0)));
        register(new ValueType(NUMBER, null));
        register(new ValueType(NUMBER_PRIMITIVE, Double.valueOf(0)));

        register(new ValueType(EMPTY_OBJECT, Collections.emptyMap(), null,
                null));

        ValueType undefined = new ValueType(UNDEFINED, null);
        register(undefined);
        register(new ValueType(UNDEFINED_ARRAY, Collections.emptyMap(), null,
                undefined));
    }

    public void register(ValueType type) {
        Integer key = Integer.valueOf(type.getId());

        assert !valueTypes.containsKey(key);

        valueTypes.put(key, type);
    }

    public ValueType get(int id) {
        ValueType valueType = valueTypes.get(Integer.valueOf(id));

        assert valueType != null;

        return valueType;
    }
}
