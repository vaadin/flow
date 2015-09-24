package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonBoolean;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonString;
import elemental.json.JsonValue;

public class JsonConverter {
    private static Map<Class<?>, Object> defaultPrimitiveValues = new HashMap<>();

    static {
        defaultPrimitiveValues.put(boolean.class, Boolean.FALSE);
        defaultPrimitiveValues.put(int.class, Integer.valueOf(0));
        defaultPrimitiveValues.put(double.class, Double.valueOf(0));
    }

    public static Object fromJson(Type targetType, JsonValue value) {
        if (value instanceof JsonNull) {
            return defaultPrimitiveValues.getOrDefault(targetType, null);
        } else {
            if (targetType == String.class && value instanceof JsonString) {
                return value.asString();
            } else if (value instanceof JsonNumber) {
                if (targetType == int.class || targetType == Integer.class) {
                    return Integer.valueOf((int) value.asNumber());
                } else if (targetType == double.class
                        || targetType == Double.class) {
                    return Double.valueOf(value.asNumber());
                }
            } else
                if ((targetType == boolean.class || targetType == Boolean.class)
                        && value instanceof JsonBoolean) {
                return Boolean.valueOf(value.asBoolean());
            }
        }

        throw new RuntimeException("Can't convert " + value.getType() + " to "
                + targetType.getTypeName());
    }

    public static JsonValue toJson(Object value) {
        if (value == null) {
            return Json.createNull();
        } else if (value instanceof String) {
            return Json.create((String) value);
        } else if (value instanceof Number) {
            return Json.create(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            return Json.create(((Boolean) value).booleanValue());
        } else if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value instanceof Enum<?>) {
            Enum<?> enumValue = (Enum<?>) value;
            return Json.create(enumValue.name());
        } else {
            throw new RuntimeException(
                    "Can't encode value of type " + value.getClass().getName());
        }

    }

    public static Type findType(JsonValue value) {
        switch (value.getType()) {
        case BOOLEAN:
            return Boolean.class;
        case NUMBER:
            return Double.class;
        case STRING:
        case NULL:
            return String.class;
        default:
            throw new RuntimeException(
                    "No simple type available for " + value.toJson());
        }
    }
}
