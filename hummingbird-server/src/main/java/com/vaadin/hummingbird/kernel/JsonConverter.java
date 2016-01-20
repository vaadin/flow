package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Date;

import com.vaadin.hummingbird.kernel.ValueType.ArrayType;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class JsonConverter {
    public static Object fromJson(Type targetType, JsonValue value) {
        return fromJson(ValueType.get(targetType), value);
    }

    public static Object fromJson(ValueType targetType, JsonValue value) {
        if (value == null || value instanceof JsonNull) {
            return targetType.getDefaultValue();
        } else {
            if (targetType == ValueType.STRING && value instanceof JsonString) {
                return value.asString();
            } else if (value instanceof JsonNumber) {
                if (targetType == ValueType.INTEGER
                        || targetType == ValueType.INTEGER_PRIMITIVE) {
                    return Integer.valueOf((int) value.asNumber());
                } else if (targetType == ValueType.NUMBER
                        || targetType == ValueType.NUMBER_PRIMITIVE) {
                    return Double.valueOf(value.asNumber());
                }
            } else if ((targetType == ValueType.BOOLEAN
                    || targetType == ValueType.BOOLEAN_PRIMITIVE)
                    && value instanceof JsonBoolean) {
                return Boolean.valueOf(value.asBoolean());
            } else if (targetType == ValueType.JSON_TYPE) {
                return value;
            } else if (value instanceof JsonArray
                    && targetType instanceof ArrayType) {
                return convertToArray((ArrayType) targetType,
                        (JsonArray) value);
            }
        }

        throw new RuntimeException(
                "Can't convert " + value.getType() + " to " + targetType);

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

    public static ValueType findValueType(JsonValue value) {
        return ValueType.get(findType(value));
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
        case ARRAY:
            return resolveArrayType((JsonArray) value);
        default:
            throw new RuntimeException(
                    "No simple type available for " + value.toJson());
        }
    }

    private static Type resolveArrayType(JsonArray array) {
        if (isArrayType(array, JsonType.STRING)) {
            return String[].class;
        } else if (isArrayType(array, JsonType.NUMBER)) {
            // number types treated always as double
            return Double[].class;
        } else if (isArrayType(array, JsonType.BOOLEAN)) {
            return Boolean[].class;
        }
        return Object[].class;
    }

    private static boolean isArrayType(JsonArray array, JsonType type) {
        for (int i = 0; i < array.length(); i++) {
            JsonType itemType = array.get(i).getType();
            if (itemType != type && itemType != JsonType.NULL) {
                return false;
            }
        }
        return true;
    }

    public static Object convertToArray(ArrayType targetType, JsonArray value) {
        Object[] array;
        ValueType memberType = targetType.getMemberType();
        if (memberType.equals(ValueType.STRING)) {
            array = new String[value.length()];
        } else if (memberType.equals(ValueType.BOOLEAN)
                || memberType.equals(ValueType.BOOLEAN_PRIMITIVE)) {
            array = new Boolean[value.length()];
        } else if (memberType.equals(ValueType.NUMBER)
                || memberType.equals(ValueType.NUMBER_PRIMITIVE)) {
            array = new Double[value.length()];
        } else if (memberType.equals(ValueType.INTEGER)
                || memberType.equals(ValueType.INTEGER_PRIMITIVE)) {
            array = new Integer[value.length()];
        } else {
            array = new Object[value.length()];
            for (int i = 0; i < value.length(); i++) {
                JsonValue item = value.get(i);
                // figure type for each item separately
                array[i] = fromJson(findType(item), item);
            }
            return array;
        }

        for (int i = 0; i < value.length(); i++) {
            JsonValue item = value.get(i);
            array[i] = fromJson(memberType, item);
        }
        return array;
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
