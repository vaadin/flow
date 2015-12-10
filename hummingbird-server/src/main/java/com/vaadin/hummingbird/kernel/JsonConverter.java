package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonArray;
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
        if (value == null || value instanceof JsonNull) {
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
            } else if ((targetType == boolean.class
                    || targetType == Boolean.class)
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
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);

            JsonArray array = Json.createArray();
            for (int i = 0; i < length; i++) {
                Object childValue = Array.get(value, i);
                JsonValue childJson = toJson(childValue);
                array.set(i, childJson);
            }

            return array;
        } else if (value instanceof Date) {
            return Json.create(((Date) value).getTime());
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

    public static boolean isSupportedType(Class<?> type) {

        if (type.isPrimitive() || type.isArray()) {
            return true;
        }
        if (Number.class.isAssignableFrom(type)) {
            return true;
        }
        if (String.class == type) {
            return true;
        }
        if (Date.class == type) {
            return true;
        }
        if (Boolean.class == type) {
            return true;
        }
        if (JsonValue.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }
}
