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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.util.ReflectTools;

/**
 * Invocation handler for {@link TemplateModel} proxy objects.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelProxyHandler implements InvocationHandler {

    private static final Class<?>[] SUPPORTED_PROPERTY_TYPES = new Class[] {
            Boolean.class, Double.class, Integer.class, String.class };

    private final StateNode stateNode;

    protected TemplateModelProxyHandler(StateNode stateNode) {
        this.stateNode = stateNode;
    }

    /**
     * Creates a proxy object for the given {@link TemplateModel} type for the
     * given template state node.
     *
     * @param stateNode
     *            the template's state node
     * @param modelType
     *            the type of the template's model
     * @return a proxy object
     */
    public static <T extends TemplateModel> T createModelProxy(
            StateNode stateNode, Class<T> modelType) {
        return modelType.cast(Proxy.newProxyInstance(modelType.getClassLoader(),
                new Class[] { modelType },
                new TemplateModelProxyHandler(stateNode)));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass == Object.class) {
            return handleObjectMethod(method, args);
        }

        if ("importBean".equals(method.getName())) {
            TemplateModelBeanUtil.importBeanIntoModel(this, args[0]);
            return null;
        }

        if (ReflectTools.isGetter(method)) {
            return handleGetter(method);
        } else if (ReflectTools.isSetter(method)) {
            return handleSetter(method, args[0]);
        }

        throw new UnsupportedOperationException(
                getUnsupportedMethodMessage(method, args));
    }

    private static String getUnsupportedMethodMessage(Method unsupportedMethod,
            Object[] args) {
        return "Template Model does not support: " + unsupportedMethod.getName()
                + " with return type: "
                + unsupportedMethod.getReturnType().getName()
                + (args == null ? " and no parameters"
                        : " with parameters: " + Stream.of(args)
                                .map(Object::getClass).map(Class::getName)
                                .collect(Collectors.joining(", ")));
    }

    private Object handleGetter(Method method) {
        ModelMap modelMap = getModelMap();
        String propertyName = ReflectTools.getPropertyName(method);
        Type returnType = method.getGenericReturnType();

        Object value = modelMap.getValue(propertyName);
        if (Boolean.class == returnType) {
            return parseBooleanValue(value);
        }

        if (returnType instanceof Class<?>) {
            Class<?> returnClazz = (Class<?>) returnType;
            if (isSupportedPrimitiveType(returnClazz)) {
                return value != null ? value
                        : getPrimitiveDefaultValue(returnClazz);
            }

            if (isSupportedType(returnClazz)) {
                return value;
            }
        }

        // only boolean, integer, double and string are currently supported
        throw new UnsupportedOperationException(
                "Template model does not yet support type "
                        + returnType.getTypeName() + " (" + method.getName()
                        + "), supported types are:"
                        + getSupportedTypesString());
    }

    private Object handleSetter(Method method, Object value) {
        String propertyName = ReflectTools.getPropertyName(method);
        Type declaredValueType = method.getGenericParameterTypes()[0];
        return setModelValue(propertyName, declaredValueType, value);
    }

    Object setModelValue(String propertyName, Type expectedType, Object value) {
        ModelMap modelMap = getModelMap();
        Object oldValue = modelMap.getValue(propertyName);
        // this might cause scenario where invalid type is not caught because
        // both values are null
        if (Objects.equals(value, oldValue)) {
            return null;
        }

        if (Boolean.class == expectedType) {
            modelMap.setValue(propertyName, parseBooleanValue(value));
            return null;
        }

        if (expectedType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) expectedType;

            if (isSupportedType(clazz) || isSupportedPrimitiveType(clazz)) {
                // all currently supported types are serializable
                modelMap.setValue(propertyName, (Serializable) value);

                return null;
            }

        }

        throw new UnsupportedOperationException(
                "Template model does not yet support type "
                        + expectedType.getTypeName() + ", supported types are:"
                        + getSupportedTypesString());
    }

    private Object handleObjectMethod(Method method, Object[] args) {
        switch (method.getName()) {
        case "equals":
            assert args.length == 1;
            return handleEquals(args[0]);
        case "hashCode":
            assert args == null;
            return Integer.valueOf(stateNode.hashCode());
        case "toString":
            assert args == null;
            return "Template Model for a state node with id "
                    + stateNode.getId();
        default:
            throw new UnsupportedOperationException(
                    "Template Model does not support: " + method);
        }
    }

    private Boolean handleEquals(Object other) {
        if (other == null || !isTemplateModelProxy(other)) {
            return Boolean.FALSE;
        }
        StateNode otherNode = getStateNodeForProxy(other);
        return Boolean.valueOf(Objects.equals(otherNode, stateNode));
    }

    private ModelMap getModelMap() {
        return stateNode.getFeature(ModelMap.class);
    }

    private static boolean isSupportedType(Class<?> clazz) {
        return Stream.of(SUPPORTED_PROPERTY_TYPES)
                .anyMatch(type -> type.isAssignableFrom(clazz));
    }

    private static boolean isSupportedPrimitiveType(Class<?> clazz) {
        return clazz.isPrimitive() && (clazz == int.class
                || clazz == double.class || clazz == boolean.class);
    }

    private static Boolean parseBooleanValue(Object modelValue) {
        if (modelValue instanceof String) {
            throw new UnsupportedOperationException(
                    "Template Model does not support parsing String to Boolean.");
        } else {
            return (Boolean) modelValue;
        }
    }

    private static boolean isTemplateModelProxy(Object proxy) {
        return Proxy.isProxyClass(proxy.getClass())
                && Proxy.getInvocationHandler(
                        proxy) instanceof TemplateModelProxyHandler;
    }

    private static StateNode getStateNodeForProxy(Object proxy) {
        InvocationHandler handler = Proxy.getInvocationHandler(proxy);
        return ((TemplateModelProxyHandler) handler).stateNode;
    }

    private static String getSupportedTypesString() {
        return Stream.of(SUPPORTED_PROPERTY_TYPES).map(Class::getName)
                .collect(Collectors.joining(", "))
                + " (and corresponding primitive types)";
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
