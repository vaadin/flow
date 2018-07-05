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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * Invocation handler for {@link TemplateModel} proxy objects.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TemplateModelProxyHandler implements Serializable {

    /**
     * Gives access to the state node of a proxy instance.
     */
    protected interface ModelProxy extends Serializable {
        /**
         * Gets the state node that this instance is backed by.
         *
         * @return the state node, not <code>null</code>
         */
        // $ in the name to minimize collision risk
        StateNode $stateNode();

        /**
         * Sets the state node that this instance is backed by.
         *
         * @param node
         *            the state node, not <code>null</code>
         */
        // $ in the name to minimize collision risk
        void $stateNode(StateNode node);

        /**
         * Gets the model type definition for this type.
         *
         * @return the model type, not <code>null</code>
         */
        BeanModelType<?> $modelType();

        /**
         * Sets the model type for this instance.
         *
         * @param type
         *            the model type, not <code>null</code>
         */
        void $modelType(BeanModelType<?> type);
    }

    /**
     * Base type used for interface proxy types.
     */
    public abstract static class InterfaceProxy implements ModelProxy {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof InterfaceProxy) {
                InterfaceProxy that = (InterfaceProxy) obj;
                return $stateNode().equals(that.$stateNode())
                        && $modelType().equals(that.$modelType());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "Template Model for a state node with id "
                    + $stateNode().getId();
        }

        @Override
        public int hashCode() {
            return Objects.hash($stateNode(), $modelType());
        }
    }

    private static final ReflectionCache<Object, BiFunction<StateNode, BeanModelType<?>, Object>> proxyConstructors = new ReflectionCache<>(
            TemplateModelProxyHandler::createProxyConstructor);

    private static final TemplateModelProxyHandler proxyHandler = new TemplateModelProxyHandler();

    private TemplateModelProxyHandler() {
        // Singleton
    }

    /**
     * Processes a method invocation on a Byte buddy proxy instance and returns
     * the result. This method will be invoked on an invocation handler when a
     * method is invoked on a proxy instance that it is associated with.
     *
     * @param target
     *            the proxy instance
     * @param method
     *            the {@code Method} instance corresponding to the proxied
     *            method invoked on the proxy instance.
     *
     * @param args
     *            an array of objects containing the values of the arguments
     *            passed in the method invocation on the proxy instance.
     * @return the value to return from the method invocation on the proxy
     *         instance.
     */
    @RuntimeType
    @SuppressWarnings("static-method")
    public Object intercept(@This Object target, @Origin Method method,
            @AllArguments Object[] args) {
        String propertyName = ReflectTools.getPropertyName(method);

        BeanModelType<?> modelType = getModelTypeForProxy(target);

        if (!modelType.hasProperty(propertyName)) {
            throw new InvalidTemplateModelException(
                    modelType.getProxyType().getName()
                            + " has no property named " + propertyName
                            + " (or it has been excluded)");
        }

        ModelType propertyType = modelType.getPropertyType(propertyName);
        ElementPropertyMap modelMap = ElementPropertyMap
                .getModel(getStateNodeForProxy(target));

        if (ReflectTools.isGetter(method)) {
            return handleGetter(modelMap, propertyName, propertyType);
        } else if (ReflectTools.isSetter(method)) {
            Object value = args[0];
            handleSetter(modelMap, propertyName, propertyType, value);
            return null;
        }

        throw new InvalidTemplateModelException(
                getUnsupportedMethodMessage(method, args));
    }

    /**
     * Creates a proxy object for the given {@code modelType} type for the given
     * state node.
     *
     * @param <T>
     *            the proxy type
     * @param stateNode
     *            the state node, not <code>null</code>
     * @param modelType
     *            the type of the model, not <code>null</code>
     * @return a proxy object, not <code>null</code>
     */
    public static <T> T createModelProxy(StateNode stateNode,
            BeanModelType<T> modelType) {
        assert stateNode != null;
        assert modelType != null;

        Class<T> proxyType = modelType.getProxyType();

        Object proxy = proxyConstructors.get(proxyType).apply(stateNode,
                modelType);

        return proxyType.cast(proxy);
    }

    private static BiFunction<StateNode, BeanModelType<?>, Object> createProxyConstructor(
            Class<?> type) {
        if (type.isInterface()) {
            return createInterfaceConstructor(type);
        } else {
            return createClassConstructor(type);
        }
    }

    private static BiFunction<StateNode, BeanModelType<?>, Object> createInterfaceConstructor(
            Class<?> modelType) {
        Builder<InterfaceProxy> builder = new ByteBuddy()
                .subclass(InterfaceProxy.class).implement(modelType);

        return createProxyConstructor(modelType.getClassLoader(), builder,
                modelType.getCanonicalName());
    }

    private static BiFunction<StateNode, BeanModelType<?>, Object> createClassConstructor(
            Class<?> modelType) {
        Builder<?> builder = new ByteBuddy().subclass(modelType)
                .implement(ModelProxy.class);

        return createProxyConstructor(modelType.getClassLoader(), builder,
                modelType.getCanonicalName());
    }

    private static BiFunction<StateNode, BeanModelType<?>, Object> createProxyConstructor(
            ClassLoader classLoader, Builder<?> proxyBuilder, String classFqn) {
        String proxyClassName = generateProxyClassName(classFqn, classLoader);
        Class<?> proxyType = proxyBuilder

                // Handle bean methods (and abstract methods for error handling)
                .method(method -> isAccessor(method) || method.isAbstract())
                .intercept(MethodDelegation.to(proxyHandler))

                // Handle internal $stateNode methods
                .defineField("$stateNode", StateNode.class)
                .method(method -> "$stateNode".equals(method.getName()))
                .intercept(FieldAccessor.ofField("$stateNode"))

                // Handle internal $modelType methods
                .defineField("$modelType", BeanModelType.class)
                .method(method -> "$modelType".equals(method.getName()))
                .intercept(FieldAccessor.ofField("$modelType"))

                // Create the class
                .name(proxyClassName).make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        return (node, modelType) -> {
            Object instance = ReflectTools.createProxyInstance(proxyType,
                    modelType.getProxyType());
            ModelProxy modelProxy = (ModelProxy) instance;
            modelProxy.$stateNode(node);
            modelProxy.$modelType(modelType);

            modelType.createInitialValues(node);
            return instance;
        };
    }

    private static String generateProxyClassName(String classFqn,
            ClassLoader classLoader) {
        StringBuilder fqnBuilder = new StringBuilder(classFqn);
        boolean classExists = true;
        do {
            fqnBuilder.append('$');
            try {
                Class.forName(fqnBuilder.toString(), false, classLoader);
            } catch (ClassNotFoundException exception) {
                classExists = false;
            }
        } while (classExists);
        return fqnBuilder.toString();
    }

    private static boolean isAccessor(MethodDescription method) {
        if (method.getDeclaringType().represents(Object.class)) {
            return false;
        }
        String methodName = method.getName();
        Generic returnType = method.getReturnType();
        ParameterList<?> args = method.getParameters();

        boolean isSetter = Generic.VOID.equals(returnType) && args.size() == 1
                && ReflectTools.isSetterName(methodName);
        boolean isGetter = !Generic.VOID.equals(returnType) && args.isEmpty()
                && ReflectTools.isGetterName(methodName,
                        returnType.represents(boolean.class));
        return isSetter || isGetter;
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

    private static Object handleGetter(ElementPropertyMap modelMap,
            String propertyName, ModelType propertyType) {
        Serializable modelValue = modelMap.getProperty(propertyName);

        try {
            return propertyType.modelToApplication(modelValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(String.format(
                    "Model property '%s' has an unexpected stored value: %s",
                    propertyName, exception.getMessage()), exception);
        }
    }

    private static void handleSetter(ElementPropertyMap modelMap,
            String propertyName, ModelType propertyType, Object value) {
        Serializable modelValue = propertyType.applicationToModel(value,
                PropertyFilter.ACCEPT_ALL);

        modelMap.setProperty(propertyName, modelValue);
    }

    /**
     * Gets the state node that a proxy is bound to.
     *
     * @param proxy
     *            the template model proxy
     * @return the state node of the proxy
     */
    public static StateNode getStateNodeForProxy(Object proxy) {
        return assertIsProxy(proxy).$stateNode();
    }

    /**
     * Gets the model type that a proxy instance is bound to.
     *
     * @param proxy
     *            the template model proxy
     * @return the model type of the proxy
     */
    public static BeanModelType<?> getModelTypeForProxy(Object proxy) {
        return assertIsProxy(proxy).$modelType();
    }

    private static ModelProxy assertIsProxy(Object maybeProxy) {
        if (!isProxy(maybeProxy)) {
            throw new IllegalArgumentException(
                    maybeProxy + " is not a template model proxy");
        } else {
            return (ModelProxy) maybeProxy;
        }
    }

    /**
     * Checks if the given object is a proxy created by this class.
     *
     * @param proxy
     *            the object to check
     * @return <code>true</code> if the given object is a proxy object,
     *         <code>false</code> otherwise
     */
    public static boolean isProxy(Object proxy) {
        return proxy instanceof ModelProxy;
    }

}
