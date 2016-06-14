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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.util.ReflectionCache;
import com.vaadin.util.ReflectTools;

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
 */
public class TemplateModelProxyHandler implements Serializable {

    /**
     * Gives access to the state node of a proxy instance.
     */
    protected interface ModelProxy {
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
                return $stateNode().equals(that.$stateNode());
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
            return $stateNode().hashCode();
        }
    }

    private static final ReflectionCache<Object, Function<StateNode, Object>> proxyConstructors = new ReflectionCache<>(
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
        StateNode stateNode = getStateNodeForProxy(target);

        if (ReflectTools.isGetter(method)) {
            return handleGetter(stateNode, method);
        } else if (ReflectTools.isSetter(method)) {
            handleSetter(stateNode, method, args[0]);
            return null;
        }

        throw new InvalidTemplateModelException(
                getUnsupportedMethodMessage(method, args));
    }

    /**
     * Creates a proxy object for the given {@link TemplateModel} type for the
     * given template state node.
     *
     * @param stateNode
     *            the template's state node, not <code>null</code>
     * @param modelType
     *            the type of the template's model, not <code>null</code>
     * @return a proxy object, not <code>null</code>
     */
    public static <T extends TemplateModel> T createTemplateModelProxy(
            StateNode stateNode, Class<T> modelType) {
        ModelMap model = getModelMap(stateNode, modelType);
        TemplateModelBeanUtil.populateProperties(model, modelType);
        return createModelProxy(stateNode, modelType);
    }

    /**
     * Creates a proxy object for the given {@code modelType} type for the given
     * state node.
     *
     * @param stateNode
     *            the state node, not <code>null</code>
     * @param modelType
     *            the type of the model, not <code>null</code>
     * @return a proxy object, not <code>null</code>
     */
    public static <T> T createModelProxy(StateNode stateNode,
            Class<T> modelType) {
        getModelMap(stateNode, modelType);

        return modelType
                .cast(proxyConstructors.get(modelType).apply(stateNode));
    }

    private static <T> ModelMap getModelMap(StateNode stateNode,
            Class<T> modelType) {
        assert stateNode != null;
        assert modelType != null;

        ModelMap model = stateNode.getFeature(ModelMap.class);
        if (model == null) {
            throw new IllegalArgumentException(
                    "Provided StateNode doesn't have a model");
        }
        return model;
    }

    private static Function<StateNode, Object> createProxyConstructor(
            Class<?> type) {
        if (type.isInterface()) {
            return createInterfaceConstructor(type);
        } else {
            return createClassConstructor(type);
        }
    }

    private static Function<StateNode, Object> createInterfaceConstructor(
            Class<?> modelType) {
        Builder<InterfaceProxy> builder = new ByteBuddy()
                .subclass(InterfaceProxy.class).implement(modelType);

        return createProxyConstructor(modelType.getClassLoader(), builder);
    }

    private static Function<StateNode, Object> createClassConstructor(
            Class<?> modelType) {
        Builder<?> builder = new ByteBuddy().subclass(modelType)
                .implement(ModelProxy.class);

        return createProxyConstructor(modelType.getClassLoader(), builder);
    }

    private static Function<StateNode, Object> createProxyConstructor(
            ClassLoader classLoader, Builder<?> proxyBuilder) {
        Class<?> proxyType = proxyBuilder

                // Handle bean methods (and abstract methods for error handling)
                .method(method -> isAccessor(method) || method.isAbstract())
                .intercept(MethodDelegation.to(proxyHandler))

                // Handle internal $stateNode methods
                .defineField("$stateNode", StateNode.class)
                .method(method -> "$stateNode".equals(method.getName()))
                .intercept(FieldAccessor.ofField("$stateNode"))

                // Create the class
                .make().load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        return node -> {
            Object instance = ReflectTools.createInstance(proxyType);
            ((ModelProxy) instance).$stateNode(node);
            return instance;
        };
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

    private static Object handleGetter(StateNode stateNode, Method method) {
        ModelMap modelMap = getModelMap(stateNode);
        String propertyName = ReflectTools.getPropertyName(method);
        Type returnType = method.getGenericReturnType();

        return TemplateModelBeanUtil.getModelValue(modelMap, propertyName,
                returnType);
    }

    private static void handleSetter(StateNode stateNode, Method method,
            Object value) {
        ModelMap modelMap = getModelMap(stateNode);
        String propertyName = ReflectTools.getPropertyName(method);
        Type declaredValueType = method.getGenericParameterTypes()[0];

        TemplateModelBeanUtil.setModelValue(modelMap, propertyName,
                declaredValueType, value, "", string -> true);
    }

    private static ModelMap getModelMap(StateNode stateNode) {
        return stateNode.getFeature(ModelMap.class);
    }

    /**
     * Gets the state node that a proxy is bound to.
     *
     * @param proxy
     *            the template model proxy
     * @return the state node of the proxy
     */
    public static StateNode getStateNodeForProxy(Object proxy) {
        if (isProxy(proxy)) {
            ModelProxy model = (ModelProxy) proxy;
            return model.$stateNode();
        } else {
            throw new IllegalArgumentException(
                    "Proxy is not a proper template model proxy");
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
