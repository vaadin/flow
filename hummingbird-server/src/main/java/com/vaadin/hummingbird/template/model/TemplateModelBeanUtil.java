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
    private static final class BeanValueWrapper {
        private Method beanGetter;
        private Object value;

        public BeanValueWrapper(Method beanGetter, Object value) {
            this.beanGetter = beanGetter;
            this.value = value;
        }
    }

    private TemplateModelBeanUtil() {
        // NOOP
    }

    static void importBeanIntoModel(Supplier<StateNode> stateNodeSupplier,
            Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("Bean cannot be null");
        }

        Method[] getterMethods = getGetterMethods(bean.getClass())
                .toArray(Method[]::new);
        if (getterMethods.length == 0) {
            throw new IllegalArgumentException("Given object of type "
                    + bean.getClass().getName()
                    + " is not a Bean - it has no public getter methods!");
        }

        Stream<BeanValueWrapper> values = Stream.of(getterMethods)
                .map(method -> mapBeanValueToProperty(method, bean));

        // don't resolve the state node used until all the bean values have been
        // resolved properly
        ModelMap modelMap = stateNodeSupplier.get().getFeature(ModelMap.class);

        values.forEach(wrapper -> setModelValue(wrapper, modelMap));
    }

    static void importBeanIntoModel(StateNode parentNode, Object bean,
            String beanPath) {

        importBeanIntoModel(() -> resolveStateNode(parentNode, beanPath), bean);
    }

    private static void setModelValue(BeanValueWrapper beanValueWrapper,
            ModelMap targetModelMap) {
        setModelValue(targetModelMap,
                ReflectTools.getPropertyName(beanValueWrapper.beanGetter),
                beanValueWrapper.beanGetter.getGenericReturnType(),
                beanValueWrapper.value);
    }

    static void setModelValue(ModelMap modelMap, String propertyName,
            Type expectedType, Object value) {
        Object oldValue = modelMap.getValue(propertyName);
        // this might cause scenario where invalid type is not caught because
        // both values are null
        if (Objects.equals(value, oldValue)) {
            return;
        }

        if (Boolean.class == expectedType) {
            modelMap.setValue(propertyName, parseBooleanValue(value));
            return;
        }

        if (expectedType instanceof Class<?>) {
            setModelValueBasicType(modelMap, propertyName,
                    (Class<?>) expectedType, value);
            return;
        }

        throwUnsupportedTypeException(expectedType, propertyName);
    }

    private static void setModelValueBasicType(ModelMap modelMap,
            String propertyName, Class<?> expectedType, Object value) {
        if (isSupportedType(expectedType)) {
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
        } else {
            // handle other types as beans
            importBeanIntoModel(modelMap.getNode(), value, propertyName);
            return;
        }

        throwUnsupportedTypeException(expectedType, propertyName);
    }

    static Object getModelValue(ModelMap modelMap, String propertyName,
            Type returnType) {
        Object value = modelMap.getValue(propertyName);
        if (Boolean.class == returnType) {
            return parseBooleanValue(value);
        }

        if (returnType instanceof Class<?>) {
            Class<?> returnClazz = (Class<?>) returnType;

            if (isSupportedType(returnClazz)) {
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

        }

        return throwUnsupportedTypeException(returnType, propertyName);
    }

    private static BeanValueWrapper mapBeanValueToProperty(Method getterMethod,
            Object bean) {
        try {
            Object value = getterMethod.invoke(bean, (Object[]) null);
            return new BeanValueWrapper(getterMethod, value);
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

    private static Stream<Method> getGetterMethods(Class<?> bean) {
        return Stream.of(bean.getMethods()).filter(ReflectTools::isGetter)
                .filter(TemplateModelBeanUtil::isNotObjectMethod);
    }

    private static StateNode createModelMap(ModelMap parent,
            String propertyName) {
        StateNode node = new StateNode(ModelMap.class);
        parent.setValue(propertyName, node);
        return node;
    }

    private static boolean isNotObjectMethod(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass != Object.class;
    }

    private static String getSupportedTypesString() {
        return Stream.of(SUPPORTED_PROPERTY_TYPES).map(Class::getName)
                .collect(Collectors.joining(", "))
                + " (and corresponding primitive types)";
    }

    private static boolean isSupportedType(Class<?> clazz) {
        return Stream.of(SUPPORTED_PROPERTY_TYPES)
                .anyMatch(type -> type.isAssignableFrom(clazz));
    }

    private static boolean isSupportedPrimitiveType(Class<?> primitiveType) {
        return isSupportedType(
                ReflectTools.convertPrimitiveType(primitiveType));
    }

    private static Object throwUnsupportedTypeException(Type type,
            String propertyName) {
        throw new UnsupportedOperationException(
                "Template model does not yet support type " + type.getTypeName()
                        + " (" + propertyName + "), supported types are:"
                        + getSupportedTypesString());
    }

    private static Boolean parseBooleanValue(Object modelValue) {
        if (modelValue instanceof String) {
            throw new UnsupportedOperationException(
                    "Template Model does not support parsing String to Boolean.");
        } else {
            return (Boolean) modelValue;
        }
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
                "Template model does not yet support primitive type "
                        + primitiveType.getName()
                        + ", all supported types are: "
                        + getSupportedTypesString());
    }
}
