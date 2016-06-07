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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.NodeFeature;
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
            Class<?> beanType, Object bean, String pathPrefix,
            Predicate<String> filter) {
        assert pathPrefix != null
                && (pathPrefix.isEmpty() || pathPrefix.endsWith("."));
        assert beanType != null;

        if (bean == null) {
            throw new IllegalArgumentException("Bean cannot be null");
        }

        Method[] getterMethods = ReflectTools.getGetterMethods(beanType)
                .toArray(Method[]::new);
        if (getterMethods.length == 0) {
            throw new IllegalArgumentException("Given type "
                    + beanType.getName()
                    + " is not a Bean - it has no public getter methods!");
        }

        Predicate<Method> methodBasedFilter = method -> filter
                .test(pathPrefix + ReflectTools.getPropertyName(method));

        List<ModelPropertyWrapper> values = Stream.of(getterMethods)
                .filter(methodBasedFilter)
                .map(method -> mapBeanValueToProperty(method, bean))
                .collect(Collectors.toList());

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
        } else if (expectedType instanceof ParameterizedType) {
            setModelValueParameterizedType(modelMap, propertyName, expectedType,
                    value);
            return;
        }

        throw createUnsupportedTypeException(expectedType, propertyName);
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
                    () -> resolveStateNode(modelMap.getNode(), propertyName,
                            ModelMap.class),
                    expectedType, value, newPathPrefix, filter);
            return;
        }
    }

    private static void setModelValueParameterizedType(ModelMap modelMap,
            String propertyName, Type expectedType, Object value) {
        ParameterizedType pt = (ParameterizedType) expectedType;

        if (pt.getRawType() != List.class) {
            throw createUnsupportedTypeException(expectedType, propertyName);
        }

        Type itemType = pt.getActualTypeArguments()[0];
        if (!(itemType instanceof Class<?>)) {
            throw createUnsupportedTypeException(expectedType, propertyName);
        }

        Class<?> itemClass = (Class<?>) itemType;

        if (isSupportedNonPrimitiveType(itemClass)) {
            throw createUnsupportedTypeException(expectedType, propertyName);
        }

        importListIntoModel(modelMap.getNode(), (List<?>) value, itemClass,
                propertyName);
    }

    private static void importListIntoModel(StateNode parentNode, List<?> list,
            Class<?> itemType, String propertyName) {

        // Collect all child nodes before trying to resolve the list node
        List<StateNode> childNodes = new ArrayList<>();
        for (Object bean : list) {
            StateNode childNode = TemplateElementStateProvider
                    .createSubModelNode(ModelMap.class);
            importBeanIntoModel(() -> childNode, itemType, bean, "",
                    path -> true);
            childNodes.add(childNode);
        }

        ModelList modelList = resolveStateNode(parentNode, propertyName,
                ModelList.class).getFeature(ModelList.class);
        modelList.clear();

        modelList.addAll(childNodes);
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
        }
        // not supported primitive, throw exception for now
        // or #731 consider as a "sub" model or nested bean and return a
        // new proxy to model
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
            String childNodePath, Class<? extends NodeFeature> childFeature) {
        ModelMap parentLevel = parentNode.getFeature(ModelMap.class);
        if (parentLevel.hasValue(childNodePath)) {
            Serializable value = parentLevel.getValue(childNodePath);
            if (value instanceof StateNode
                    && ((StateNode) value).hasFeature(childFeature)) {
                // reuse old one
                return (StateNode) value;
            } else {
                // just override
                return createSubModel(parentLevel, childNodePath, childFeature);
            }
        } else {
            return createSubModel(parentLevel, childNodePath, childFeature);
        }
    }

    private static StateNode createSubModel(ModelMap parent,
            String propertyName, Class<? extends NodeFeature> childFeature) {
        StateNode node = TemplateElementStateProvider
                .createSubModelNode(childFeature);
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
                        + getSupportedTypesString()
                        + ", beans and lists of beans.");
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
