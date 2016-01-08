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
        valueTypes.put(Integer.valueOf(STRING), new ValueType(null));
        valueTypes.put(Integer.valueOf(BOOLEAN), new ValueType(null));
        valueTypes.put(Integer.valueOf(BOOLEAN_PRIMITIVE),
                new ValueType(Boolean.FALSE));
        valueTypes.put(Integer.valueOf(INTEGER), new ValueType(null));
        valueTypes.put(Integer.valueOf(INTEGER_PRIMITIVE),
                new ValueType(Integer.valueOf(0)));
        valueTypes.put(Integer.valueOf(NUMBER), new ValueType(null));
        valueTypes.put(Integer.valueOf(NUMBER_PRIMITIVE),
                new ValueType(Double.valueOf(0)));

        valueTypes.put(Integer.valueOf(EMPTY_OBJECT),
                new ValueType(Collections.emptyMap(), null));

        ValueType undefined = new ValueType(null);
        valueTypes.put(Integer.valueOf(UNDEFINED), undefined);
        valueTypes.put(Integer.valueOf(UNDEFINED_ARRAY),
                new ValueType(Collections.emptyMap(), undefined));
    }

    public void register(int id, ValueType type) {
        Integer key = Integer.valueOf(id);

        assert !valueTypes.containsKey(key);

        valueTypes.put(key, type);
    }

    public ValueType get(int id) {
        ValueType valueType = valueTypes.get(Integer.valueOf(id));

        assert valueType != null;

        return valueType;
    }
}
