/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.JsonSerializable;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNull;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * General-purpose serializer of Java objects to {@link JsonValue} and
 * deserializer of JsonValue to Java objects.
 *
 * @since 1.0
 */
public final class JsonSerializer {

    private JsonSerializer() {
    }

    /**
     * Converts a Java bean, {@link JsonSerializable} instance, String, wrapper
     * of primitive type or enum to a {@link JsonValue}.
     * <p>
     * When a bean is used, a {@link JsonObject} is returned.
     * 
     * @param bean
     *            Java object to be converted
     * @return the json representation of the Java object
     */
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

    /**
     * Converts a collection of object into a {@link JsonArray}, converting each
     * item of the collection individually.
     * 
     * @param beans
     *            the collection of objects to be converted
     * @return the json representation of the objects in the collectios. An
     *         empty array is returned if the input collections is
     *         <code>null</code>
     */
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
        if (bean instanceof Enum) {
            return Optional.of(Json.create(((Enum<?>) bean).name()));
        }
        if (bean instanceof JsonValue) {
            return Optional.of((JsonValue) bean);
        }
        return Optional.empty();
    }

    /**
     * Converts a JsonValue to the corresponding Java object. The Java object
     * can be a Java bean, {@link JsonSerializable} instance, String, wrapper of
     * primitive types or an enum.
     * 
     * @param type
     *            the type of the Java object convert the json to
     * @param json
     *            the json representation of the object
     * @param <T>
     *            the resulting object type
     *
     * @return the deserialized object, or <code>null</code> if the input json
     *         is <code>null</code>
     */
    public static <T> T toObject(Class<T> type, JsonValue json) {
        return toObject(type, null, json);
    }

    @SuppressWarnings("unchecked")
    private static <T> T toObject(Class<T> type, Type genericType,
            JsonValue json) {
        if (json == null || json instanceof JsonNull) {
            return null;
        }

        Optional<?> simpleType = tryToConvertFromSimpleType(type, json);
        if (simpleType.isPresent()) {
            return (T) simpleType.get();
        }

        if (Collection.class.isAssignableFrom(type)) {
            return toCollection(type, genericType, json);
        }

        T instance;
        try {
            instance = type.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not create an instance of type " + type
                            + ". Make sure it contains a default public constructor and the class is accessible.",
                    e);
        }

        try {
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
                    Class<?> parameterType = method.getParameterTypes()[0];
                    Type genericParameterType = method
                            .getGenericParameterTypes()[0];
                    Object value = toObject(parameterType, genericParameterType,
                            jsonValue);
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

    private static <T> T toCollection(Class<T> type, Type genericType,
            JsonValue json) {
        if (json.getType() != JsonType.ARRAY) {
            return null;
        }
        if (!(genericType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Cloud not infer the generic parameterized type of the collection of class: "
                            + type.getName()
                            + ". The type is no subclass of ParameterizedType: "
                            + genericType);
        }
        JsonArray array = (JsonArray) json;
        Collection<?> collection = tryToCreateCollection(type, array.length());
        if (array.length() > 0) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Class<?> parameterizedClass = (Class<?>) parameterizedType
                    .getActualTypeArguments()[0];
            collection.addAll(
                    (List) toObjects(parameterizedClass, (JsonArray) json));
        }
        return (T) collection;
    }

    /**
     * Converts a JsonArray into a collection of Java objects. The Java objects
     * can be Java beans, {@link JsonSerializable} instances, Strings, wrappers
     * of primitive types or enums.
     * 
     * @param type
     *            the type of the elements in the array
     * @param json
     *            the json representation of the objects
     * @param <T>
     *            the resulting objects types
     *
     * @return a modifiable list of converted objects. Returns an empty list if
     *         the input array is <code>null</code>
     */
    public static <T> List<T> toObjects(Class<T> type, JsonArray json) {
        if (json == null) {
            return new ArrayList<>(0);
        }
        List<T> list = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            list.add(JsonSerializer.toObject(type, json.get(i)));
        }
        return list;
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
        if (type.isEnum()) {
            return Optional.of(Enum.valueOf((Class<? extends Enum>) type,
                    json.asString()));
        }
        if (JsonValue.class.isAssignableFrom(type)) {
            return Optional.of(json);
        }
        return Optional.empty();

    }

    private static Collection<?> tryToCreateCollection(Class<?> collectionType,
            int initialCapacity) {
        if (collectionType.isInterface()) {
            if (List.class.isAssignableFrom(collectionType)) {
                return new ArrayList<>(initialCapacity);
            }
            if (Set.class.isAssignableFrom(collectionType)) {
                return new LinkedHashSet<>(initialCapacity);
            }
            throw new IllegalArgumentException(
                    "Collection type not supported: '"
                            + collectionType.getName()
                            + "'. Use Lists, Sets or concrete classes that implement java.util.Collection.");
        }
        try {
            return (Collection<?>) collectionType.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not create an instance of the collection of type "
                            + collectionType
                            + ". Make sure it contains a default public constructor and the class is accessible.",
                    e);
        }
    }

}
