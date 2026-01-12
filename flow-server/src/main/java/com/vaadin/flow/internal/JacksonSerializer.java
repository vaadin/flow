/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.JsonSerializable;

/**
 * General-purpose serializer of Java objects to {@link ObjectNode} and
 * deserializer of JsonNode to Java objects.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public final class JacksonSerializer {

    private JacksonSerializer() {
    }

    /**
     * Converts a Java bean, {@link JsonSerializable} instance, String, wrapper
     * of primitive type or enum to a {@link JsonNode}.
     * <p>
     * When a bean is used, a {@link ObjectNode} is returned.
     *
     * @param bean
     *            Java object to be converted
     * @return the json representation of the Java object
     */
    public static JsonNode toJson(Object bean) {
        if (bean == null) {
            return JacksonUtils.nullNode();
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

        Optional<JsonNode> simpleType = tryToConvertToSimpleType(bean);
        if (simpleType.isPresent()) {
            return simpleType.get();
        }

        try {
            ObjectNode json = JacksonUtils.createObjectNode();
            Class<?> type = bean.getClass();

            if (type.isRecord()) {
                for (RecordComponent rc : type.getRecordComponents()) {
                    json.set(rc.getName(),
                            toJson(rc.getAccessor().invoke(bean)));
                }
            } else {
                BeanInfo info = Introspector.getBeanInfo(type);
                for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                    if ("class".equals(pd.getName())) {
                        continue;
                    }
                    Method reader = pd.getReadMethod();
                    if (reader != null) {
                        json.set(pd.getName(), toJson(reader.invoke(bean)));
                    }
                }
            }

            return json;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not serialize object of type " + bean.getClass()
                            + " to JsonNode",
                    e);
        }
    }

    /**
     * Converts a collection of object into a {@link ArrayNode}, converting each
     * item of the collection individually.
     *
     * @param beans
     *            the collection of objects to be converted
     * @return the json representation of the objects in the collectios. An
     *         empty array is returned if the input collections is
     *         <code>null</code>
     */
    public static ArrayNode toJson(Collection<?> beans) {
        ArrayNode array = JacksonUtils.createArrayNode();
        if (beans == null) {
            return array;
        }

        beans.stream().map(JacksonSerializer::toJson)
                .forEachOrdered(json -> array.add(json));
        return array;
    }

    private static ArrayNode toJsonArray(Object javaArray) {
        int length = Array.getLength(javaArray);
        ArrayNode array = JacksonUtils.createArrayNode();
        for (int i = 0; i < length; i++) {
            array.set(i, toJson(Array.get(javaArray, i)));
        }
        return array;
    }

    private static Optional<JsonNode> tryToConvertToSimpleType(Object bean) {
        if (bean instanceof String) {
            return Optional.of(JacksonUtils.createNode((String) bean));
        }
        if (bean instanceof Number) {
            return Optional
                    .of(JacksonUtils.createNode(((Number) bean).doubleValue()));
        }
        if (bean instanceof Boolean) {
            return Optional.of(JacksonUtils.createNode((Boolean) bean));
        }
        if (bean instanceof Character) {
            return Optional.of(
                    JacksonUtils.createNode(Character.toString((char) bean)));
        }
        if (bean instanceof Enum) {
            return Optional
                    .of(JacksonUtils.createNode(((Enum<?>) bean).name()));
        }
        if (bean instanceof JsonNode) {
            return Optional.of((JsonNode) bean);
        }
        return Optional.empty();
    }

    /**
     * Converts a JsonNode to the corresponding Java object. The Java object can
     * be a Java bean, {@link JsonSerializable} instance, String, wrapper of
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
    public static <T> T toObject(Class<T> type, JsonNode json) {
        return toObject(type, null, json);
    }

    @SuppressWarnings("unchecked")
    private static <T> T toObject(Class<T> type, Type genericType,
            JsonNode json) {
        if (json == null || json instanceof NullNode) {
            return null;
        }

        Optional<?> simpleType = tryToConvertFromSimpleType(type, json);
        if (simpleType.isPresent()) {
            return (T) simpleType.get();
        }

        if (Collection.class.isAssignableFrom(type)) {
            return toCollection(type, genericType, json);
        }

        if (type.isRecord()) {
            return toRecord(type, json);
        }

        T instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not create an instance of type " + type
                            + ". Make sure it contains a default public constructor and the class is accessible.",
                    e);
        }

        try {
            if (instance instanceof JsonSerializable
                    && json instanceof JsonNode) {
                return type.cast(JsonSerializable.class.cast(instance)
                        .readJson((JsonNode) json));
            }

            JsonNode jsonObject = json;
            List<String> keys = JacksonUtils.getKeys(jsonObject);
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
                JsonNode jsonValue = jsonObject.get(key);

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
                            + " from JsonNode",
                    e);
        }
    }

    private static <T> T toRecord(Class<T> type, JsonNode json) {
        try {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] componentTypes = new Class<?>[components.length];
            Object[] values = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                componentTypes[i] = components[i].getType();
                values[i] = toObject(componentTypes[i],
                        json.get(components[i].getName()));
            }

            return type.getDeclaredConstructor(componentTypes)
                    .newInstance(values);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not deserialize record of type " + type
                            + " from JsonNode",
                    e);
        }
    }

    private static <T> T toCollection(Class<T> type, Type genericType,
            JsonNode json) {
        if (json.getNodeType() != JsonNodeType.ARRAY) {
            return null;
        }
        if (!(genericType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Cloud not infer the generic parameterized type of the collection of class: "
                            + type.getName()
                            + ". The type is no subclass of ParameterizedType: "
                            + genericType);
        }
        ArrayNode array = (ArrayNode) json;
        Collection<?> collection = tryToCreateCollection(type, array.size());
        if (array.size() > 0) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Class<?> parameterizedClass = (Class<?>) parameterizedType
                    .getActualTypeArguments()[0];
            collection.addAll(
                    (List) toObjects(parameterizedClass, (ArrayNode) json));
        }
        return (T) collection;
    }

    /**
     * Converts a ArrayNode into a collection of Java objects. The Java objects
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
    public static <T> List<T> toObjects(Class<T> type, ArrayNode json) {
        if (json == null) {
            return new ArrayList<>(0);
        }
        List<T> list = new ArrayList<>(json.size());
        for (int i = 0; i < json.size(); i++) {
            list.add(JacksonSerializer.toObject(type, json.get(i)));
        }
        return list;
    }

    private static Optional<?> tryToConvertFromSimpleType(Class<?> type,
            JsonNode json) {
        if (type.isAssignableFrom(String.class)) {
            return Optional.of(json.asString());
        }
        if (type.isAssignableFrom(int.class)
                || type.isAssignableFrom(Integer.class)) {
            return Optional.of(json.intValue());
        }
        if (type.isAssignableFrom(double.class)
                || type.isAssignableFrom(Double.class)) {
            return Optional.of(json.doubleValue());
        }
        if (type.isAssignableFrom(long.class)
                || type.isAssignableFrom(Long.class)) {
            return Optional.of(json.longValue());
        }
        if (type.isAssignableFrom(short.class)
                || type.isAssignableFrom(Short.class)) {
            return Optional.of(json.shortValue());
        }
        if (type.isAssignableFrom(byte.class)
                || type.isAssignableFrom(Byte.class)) {
            return Optional.of(json.numberValue().byteValue());
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
        if (JsonNode.class.isAssignableFrom(type)) {
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
            return (Collection<?>) collectionType.getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not create an instance of the collection of type "
                            + collectionType
                            + ". Make sure it contains a default public constructor and the class is accessible.",
                    e);
        }
    }

}
