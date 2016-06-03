/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.util.ReflectTools;

/**
 * Utility class for mapping Bean values to {@link TemplateModel} values.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelBeanUtil {

    private static final Class<?>[] SUPPORTED_PROPERTY_TYPES = new Class[] {
            Boolean.class, Double.class, Integer.class, String.class };

    /**
     * Internal implementation of Pair / Tuple that encapsulates a value and the
     * method that it was retrieved from.
     */
    private static final class ModelPropertyWrapper {
        private String propertyName;
        private Type propertyType;
        private Object value;

        public ModelPropertyWrapper(String propertyName, Type propertyType,
                Object value) {
            this.propertyName = propertyName;
            this.propertyType = propertyType;
            this.value = value;
        }
    }

    private TemplateModelBeanUtil() {
        // NOOP
    }

    static void importBeanIntoModel(Supplier<StateNode> stateNodeSupplier,
            Object bean, String pathPrefix, Predicate<String> filter) {
        assert pathPrefix != null
                && (pathPrefix.isEmpty() || pathPrefix.endsWith("."));

        if (bean == null) {
            throw new IllegalArgumentException("Bean cannot be null");
        }

        Method[] getterMethods = ReflectTools.getGetterMethods(bean.getClass())
                .toArray(Method[]::new);
        if (getterMethods.length == 0) {
            throw new IllegalArgumentException("Given object of type "
                    + bean.getClass().getName()
                    + " is not a Bean - it has no public getter methods!");
        }

        Predicate<Method> methodBasedFilter = method -> filter
                .test(pathPrefix + ReflectTools.getPropertyName(method));

        Stream<ModelPropertyWrapper> values = Stream.of(getterMethods)
                .filter(methodBasedFilter)
                .map(method -> mapBeanValueToProperty(method, bean));

        // don't resolve the state node used until all the bean values have been
        // resolved properly
        ModelMap modelMap = stateNodeSupplier.get().getFeature(ModelMap.class);

        values.forEach(wrapper -> setModelValue(wrapper, modelMap, pathPrefix,
                filter));
    }

    private static void setModelValue(ModelPropertyWrapper modelPropertyWrapper,
            ModelMap targetModelMap, String pathPrefix,
            Predicate<String> filter) {
        setModelValue(targetModelMap, modelPropertyWrapper.propertyName,
                modelPropertyWrapper.propertyType, modelPropertyWrapper.value,
                pathPrefix, filter);
    }

    static void setModelValue(ModelMap modelMap, String propertyName,
            Type expectedType, Object value, String pathPrefix,
            Predicate<String> filter) {
        Object oldValue = modelMap.getValue(propertyName);
        // this might cause scenario where invalid type is not
        // caught because
        // both values are null
        if (Objects.equals(value, oldValue)) {
            return;
        }

        if (expectedType instanceof Class<?>) {
            setModelValueBasicType(modelMap, propertyName,
                    (Class<?>) expectedType, value, pathPrefix, filter);
            return;
        }

        throw createUnsupportedTypeException(expectedType, propertyName);
    }

    static Object getProxy(StateNode stateNode, Object[] args) {
        assert args.length == 2;
        if (args[0] == null || args[1] == null) {
            throw new IllegalArgumentException(
                    "Method getProxy() may not accept null arguments");
        }
        assert args[0] instanceof String;
        assert args[1] instanceof Class<?>;
        String modelPath = (String) args[0];
        Class<?> beanClass = (Class<?>) args[1];

        if (modelPath.isEmpty()) {
            return TemplateModelProxyHandler.createModelProxy(stateNode,
                    beanClass);
        }

        StateNode node = stateNode;
        StringTokenizer tokenizer = new StringTokenizer(modelPath, ".");
        String last = null;
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();
            node = resolveStateNode(node, name);
            last = name;
        }
        return getModelValue(node.getParent().getFeature(ModelMap.class), last,
                beanClass);
    }

    private static void setModelValueBasicType(ModelMap modelMap,
            String propertyName, Class<?> expectedType, Object value,
            String pathPrefix, Predicate<String> filter) {
        if (isSupportedNonPrimitiveType(expectedType)) {
            modelMap.setValue(propertyName, (Serializable) value);
            return;
        }

        // primitives have different "default" values than their boxed
        // versions, e.g. boolean is false and Boolean is null
        if (expectedType.isPrimitive()) {
            if (isSupportedPrimitiveType(expectedType)) {
                modelMap.setValue(propertyName, (Serializable) value);
                return;
            }
            // not supported primitive, throw exception for now
            throw createUnsupportedTypeException(expectedType, propertyName);
        } else {
            // handle other types as beans
            String newPathPrefix = pathPrefix + propertyName + ".";
            importBeanIntoModel(
                    () -> resolveStateNode(modelMap.getNode(), propertyName),
                    value, newPathPrefix, filter);
            return;
        }
    }

    static Object getModelValue(ModelMap modelMap, String propertyName,
            Type returnType) {
        Object value = modelMap.getValue(propertyName);
        if (returnType instanceof Class<?>) {
            return getModelValueBasicType(value, propertyName,
                    (Class<?>) returnType);
        }

        throw createUnsupportedTypeException(returnType, propertyName);
    }

    private static Object getModelValueBasicType(Object value,
            String propertyName, Class<?> returnClazz) {
        if (isSupportedNonPrimitiveType(returnClazz)) {
            return value;
        }

        // primitives have different "default" values than their boxed
        // versions, e.g. boolean is false and Boolean is null
        if (returnClazz.isPrimitive()
                && isSupportedPrimitiveType(returnClazz)) {
            return value != null ? value
                    : getPrimitiveDefaultValue(returnClazz);
        } else if (!returnClazz.isPrimitive() && value instanceof StateNode) {
            return TemplateModelProxyHandler.createModelProxy((StateNode) value,
                    returnClazz);
        }
        throw createUnsupportedTypeException(returnClazz, propertyName);
    }

    private static ModelPropertyWrapper mapBeanValueToProperty(
            Method getterMethod, Object bean) {
        try {
            Object value = getterMethod.invoke(bean, (Object[]) null);
            return new ModelPropertyWrapper(
                    ReflectTools.getPropertyName(getterMethod),
                    getterMethod.getGenericReturnType(), value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Given method was not accesible "
                    + bean.getClass().getName() + "::" + getterMethod.getName(),
                    e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Exception thrown while reading bean value from "
                            + bean.getClass().getName() + "::"
                            + getterMethod.getName()
                            + ", getters should not throw exceptions.",
                    e);
        }
    }

    private static StateNode resolveStateNode(StateNode parentNode,
            String childNodePath) {
        ModelMap parentLevel = parentNode.getFeature(ModelMap.class);
        if (parentLevel.hasValue(childNodePath)) {
            Serializable value = parentLevel.getValue(childNodePath);
            if (value instanceof StateNode
                    && ((StateNode) value).hasFeature(ModelMap.class)) {
                // reuse old one
                return (StateNode) value;
            } else {
                // just override
                return createModelMap(parentLevel, childNodePath);
            }
        } else {
            return createModelMap(parentLevel, childNodePath);
        }
    }

    private static StateNode createModelMap(ModelMap parent,
            String propertyName) {
        StateNode node = new StateNode(ModelMap.class);
        parent.setValue(propertyName, node);
        return node;
    }

    private static String getSupportedTypesString() {
        return Stream.of(SUPPORTED_PROPERTY_TYPES).map(Class::getName)
                .collect(Collectors.joining(", "))
                + " (and corresponding primitive types)";
    }

    private static boolean isSupportedNonPrimitiveType(Class<?> clazz) {
        return Stream.of(SUPPORTED_PROPERTY_TYPES)
                .anyMatch(type -> type.isAssignableFrom(clazz));
    }

    private static boolean isSupportedPrimitiveType(Class<?> primitiveType) {
        return isSupportedNonPrimitiveType(
                ReflectTools.convertPrimitiveType(primitiveType));
    }

    private static UnsupportedOperationException createUnsupportedTypeException(
            Type type, String propertyName) {
        return new UnsupportedOperationException(
                "Template model does not support type " + type.getTypeName()
                        + " (" + propertyName + "), supported types are:"
                        + getSupportedTypesString());
    }

    private static Object getPrimitiveDefaultValue(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return Integer.valueOf(0);
        } else if (primitiveType == double.class) {
            return Double.valueOf(0);
        } else if (primitiveType == boolean.class) {
            return false;
        }
        throw new UnsupportedOperationException(
                "Template model does not support primitive type "
                        + primitiveType.getName()
                        + ", all supported types are: "
                        + getSupportedTypesString());
    }
}