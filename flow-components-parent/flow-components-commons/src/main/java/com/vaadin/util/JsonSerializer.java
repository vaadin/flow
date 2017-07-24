package com.vaadin.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.components.JsonSerializable;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNull;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public final class JsonSerializer {

    private JsonSerializer() {
    }

    public static JsonValue toJson(Object bean) {
        if (bean == null) {
            return Json.createNull();
        }
        if (bean instanceof Collection) {
            return toJson((Collection<?>) bean);
        }
        if (bean.getClass().isArray()) {
            return toJsonArray(bean);
        }
        if (bean instanceof JsonSerializable) {
            return ((JsonSerializable) bean).toJson();
        }

        Optional<JsonValue> simpleType = tryToConvertToSimpleType(bean);
        if (simpleType.isPresent()) {
            return simpleType.get();
        }

        try {
            JsonObject json = Json.createObject();
            BeanInfo info = Introspector.getBeanInfo(bean.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if ("class".equals(pd.getName())) {
                    continue;
                }
                Method reader = pd.getReadMethod();
                if (reader != null) {
                    json.put(pd.getName(), toJson(reader.invoke(bean)));
                }
            }

            return json;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not serialize object of type " + bean.getClass()
                            + " to JsonValue",
                    e);
        }
    }

    public static JsonArray toJson(Collection<?> beans) {
        JsonArray array = Json.createArray();
        if (beans == null) {
            return array;
        }

        beans.stream().map(JsonSerializer::toJson)
                .forEachOrdered(json -> array.set(array.length(), json));
        return array;
    }

    private static JsonArray toJsonArray(Object javaArray) {
        int length = Array.getLength(javaArray);
        JsonArray array = Json.createArray();
        for (int i = 0; i < length; i++) {
            array.set(i, toJson(Array.get(javaArray, i)));
        }
        return array;
    }

    private static Optional<JsonValue> tryToConvertToSimpleType(Object bean) {
        if (bean instanceof String) {
            return Optional.of(Json.create((String) bean));
        }
        if (bean instanceof Number) {
            return Optional.of(Json.create(((Number) bean).doubleValue()));
        }
        if (bean instanceof Boolean) {
            return Optional.of(Json.create((Boolean) bean));
        }
        if (bean instanceof Character) {
            return Optional.of(Json.create(Character.toString((char) bean)));
        }
        if (bean instanceof JsonValue) {
            return Optional.of((JsonValue) bean);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T> T toObject(Class<T> type, JsonValue json) {
        if (json == null || json instanceof JsonNull) {
            return null;
        }

        Optional<?> simpleType = tryToConvertFromSimpleType(type, json);
        if (simpleType.isPresent()) {
            return (T) simpleType.get();
        }

        try {
            T instance = type.newInstance();

            if (instance instanceof JsonSerializable
                    && json instanceof JsonObject) {
                return type.cast(JsonSerializable.class.cast(instance)
                        .readJson((JsonObject) json));
            }

            JsonObject jsonObject = (JsonObject) json;
            String[] keys = jsonObject.keys();
            if (keys == null) {
                return instance;
            }

            BeanInfo info = Introspector.getBeanInfo(type);
            Map<String, Method> writers = new HashMap<>();

            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                Method writer = pd.getWriteMethod();
                if (writer != null) {
                    writers.put(pd.getName(), writer);
                }
            }
            for (String key : keys) {
                JsonValue jsonValue = jsonObject.get(key);

                Method method = writers.get(key);
                if (method != null) {
                    System.out.println(key);
                    Class<?> parameterType = method.getParameterTypes()[0];
                    Object value = toObject(parameterType, jsonValue);
                    method.invoke(instance, value);
                }
            }

            return instance;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not deserialize object of type " + type
                            + " from JsonValue",
                    e);
        }

    }

    private static Optional<?> tryToConvertFromSimpleType(Class<?> type,
            JsonValue json) {
        if (type.isAssignableFrom(String.class)) {
            return Optional.of(json.asString());
        }
        if (type.isAssignableFrom(int.class)
                || type.isAssignableFrom(Integer.class)) {
            return Optional.of((int) json.asNumber());
        }
        if (type.isAssignableFrom(double.class)
                || type.isAssignableFrom(Double.class)) {
            return Optional.of(json.asNumber());
        }
        if (type.isAssignableFrom(long.class)
                || type.isAssignableFrom(Long.class)) {
            return Optional.of((long) json.asNumber());
        }
        if (type.isAssignableFrom(short.class)
                || type.isAssignableFrom(Short.class)) {
            return Optional.of((short) json.asNumber());
        }
        if (type.isAssignableFrom(byte.class)
                || type.isAssignableFrom(Byte.class)) {
            return Optional.of((byte) json.asNumber());
        }
        if (type.isAssignableFrom(char.class)
                || type.isAssignableFrom(Character.class)) {
            return Optional.of(json.asString().charAt(0));
        }
        if (type.isAssignableFrom(Boolean.class)
                || type.isAssignableFrom(boolean.class)) {
            return Optional.of(json.asBoolean());
        }
        if (type.isAssignableFrom(JsonValue.class)) {
            return Optional.of(json);
        }
        return Optional.empty();

    }

}
