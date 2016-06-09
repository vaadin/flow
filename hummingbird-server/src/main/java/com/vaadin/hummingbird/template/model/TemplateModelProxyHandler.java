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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.util.ReflectTools;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

/**
 * Invocation handler for {@link TemplateModel} proxy objects.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelProxyHandler implements Serializable {

    private final StateNode stateNode;

    /**
     * Base type used for TemplateModel proxy types.
     */
    public static class TemplateModelBase {
        private final StateNode stateNode;

        /**
         * Creates a new proxy instance for the given state node.
         *
         * @param stateNode
         *            the state node
         */
        public TemplateModelBase(StateNode stateNode) {
            assert stateNode != null;
            this.stateNode = stateNode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof TemplateModelBase) {
                TemplateModelBase that = (TemplateModelBase) obj;
                return stateNode.equals(that.stateNode);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "Template Model for a state node with id "
                    + stateNode.getId();
        }

        @Override
        public int hashCode() {
            return stateNode.hashCode();
        }
    }

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
    public static <T> T createModelProxy(StateNode stateNode,
            Class<T> modelType) {
        TemplateModelProxyHandler proxyHandler = new TemplateModelProxyHandler(
                stateNode);
        if (TemplateModel.class.isAssignableFrom(modelType)) {
            return makeTemplateModelProxy(proxyHandler, modelType);
        } else if (modelType.isInterface()) {
            return makeInterfaceProxy(proxyHandler, modelType);
        } else {
            return makeClassProxy(proxyHandler, modelType);
        }
    }

    /**
     * Processes a method invocation on a Byte buddy proxy instance and returns
     * the result. This method will be invoked on an invocation handler when a
     * method is invoked on a proxy instance that it is associated with.
     *
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
    public Object intercept(@Origin Method method,
            @AllArguments Object[] args) {
        if (ReflectTools.isGetter(method)) {
            return handleGetter(method);
        } else if (ReflectTools.isSetter(method)) {
            return handleSetter(method, args[0]);
        }

        throw new UnsupportedOperationException(
                getUnsupportedMethodMessage(method, args));
    }

    private static <T> T makeTemplateModelProxy(
            TemplateModelProxyHandler proxyHandler, Class<T> modelType) {
        assert TemplateModel.class.isAssignableFrom(modelType);

        Builder<TemplateModelBase> builder = new ByteBuddy()
                .subclass(TemplateModelBase.class).implement(modelType);

        Class<? extends TemplateModelBase> proxyType = createModelProxyType(
                proxyHandler, modelType.getClassLoader(), builder);
        try {
            return modelType.cast(proxyType.getConstructor(StateNode.class)
                    .newInstance(proxyHandler.stateNode));
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Error instantiating model proxy", e);
        }
    }

    private static <T> T makeInterfaceProxy(
            TemplateModelProxyHandler proxyHandler, Class<T> modelType) {
        Class<? extends Object> proxyType = createModelProxyType(proxyHandler,
                modelType.getClassLoader(),
                new ByteBuddy().subclass(Object.class).implement(modelType));

        return modelType.cast(ReflectTools.createInstance(proxyType));
    }

    private static <T> T makeClassProxy(TemplateModelProxyHandler proxyHandler,
            Class<T> modelType) {
        Builder<T> subclass = new ByteBuddy().subclass(modelType);

        Class<? extends T> proxyType = createModelProxyType(proxyHandler,
                modelType.getClassLoader(), subclass);

        // createInstance checks for all the common failures, e.g. no public
        // constructor
        return ReflectTools.createInstance(proxyType);
    }

    private static <T> Class<? extends T> createModelProxyType(
            TemplateModelProxyHandler proxyHandler, ClassLoader classLoader,
            Builder<T> builder) {
        return builder
                .method(method -> isAccessor(method) || method.isAbstract())
                .intercept(MethodDelegation.to(proxyHandler)).make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
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

    private Object handleGetter(Method method) {
        ModelMap modelMap = getModelMap();
        String propertyName = ReflectTools.getPropertyName(method);
        Type returnType = method.getGenericReturnType();

        return TemplateModelBeanUtil.getModelValue(modelMap, propertyName,
                returnType);
    }

    private Object handleSetter(Method method, Object value) {
        ModelMap modelMap = getModelMap();
        String propertyName = ReflectTools.getPropertyName(method);
        Type declaredValueType = method.getGenericParameterTypes()[0];

        TemplateModelBeanUtil.setModelValue(modelMap, propertyName,
                declaredValueType, value, "", string -> true);
        return null;
    }

    private ModelMap getModelMap() {
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
        if (proxy instanceof TemplateModelBase) {
            TemplateModelBase model = (TemplateModelBase) proxy;
            return model.stateNode;
        } else {
            throw new IllegalArgumentException(
                    "Proxy is not a proper template model proxy");
        }
    }

}
