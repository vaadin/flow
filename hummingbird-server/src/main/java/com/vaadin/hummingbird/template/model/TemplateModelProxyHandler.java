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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.util.ReflectTools;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription.Generic;
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
public class TemplateModelProxyHandler implements InvocationHandler {

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
    public static <T> T createModelProxy(StateNode stateNode,
            Class<T> modelType) {
        if (modelType.isInterface()) {
            return modelType.cast(Proxy.newProxyInstance(
                    modelType.getClassLoader(), new Class[] { modelType },
                    new TemplateModelProxyHandler(stateNode)));
        } else {
            return makeClassProxy(stateNode, modelType);
        }
    }

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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass == Object.class) {
            return handleObjectMethod(method, args);
        }

        if (isTemplateModelDefaultMethod(method)) {
            return handleTemplateModelDefaultMethods(method, args);
        }

        return intercept(method, args);
    }

    private static <T> T makeClassProxy(StateNode stateNode,
            Class<T> modelType) {
        Class<? extends T> proxy = new ByteBuddy().subclass(modelType)
                .method(TemplateModelProxyHandler::isAccessor)
                .intercept(MethodDelegation
                        .to(new TemplateModelProxyHandler(stateNode)))
                .make().load(modelType.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Optional<Constructor<?>> defaultCtor = Stream
                .of(modelType.getConstructors())
                .filter((c) -> c.getParameterCount() == 0).findFirst();
        if (defaultCtor.isPresent()
                && Modifier.isPublic(defaultCtor.get().getModifiers())) {
            try {
                return proxy.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(String.format(
                        "Exception is thrown during class '%s' instantiation",
                        modelType.getName()), e);
            } catch (IllegalAccessException e) {
                // this should not happen
                throw new RuntimeException(String.format(
                        "Default public constructor in class '%s' "
                                + "is not accessable. Implementation is wrong",
                        modelType.getName()), e);
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Class '%s' doesn't declare public default constructor. "
                            + "Don't know how to instantiate it.",
                    modelType.getName()));
        }
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
        boolean isGetter = !Generic.VOID.equals(returnType) && args.size() == 0
                && ReflectTools.isGetterName(methodName,
                        returnType.represents(boolean.class));
        return isSetter || isGetter;
    }

    private static boolean isTemplateModelDefaultMethod(Method method) {
        return method.isDefault()
                && method.getDeclaringClass() == TemplateModel.class;
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

    @SuppressWarnings("unchecked")
    private Object handleTemplateModelDefaultMethods(Method method,
            Object[] args) {
        if ("importBean".equals(method.getName())) {
            switch (args.length) {
            case 1:
                TemplateModelBeanUtil.importBeanIntoModel(() -> stateNode,
                        args[0], "", propertyName -> true);
                break;
            case 2:
                TemplateModelBeanUtil.importBeanIntoModel(() -> stateNode,
                        args[0], "", (Predicate<String>) args[1]);
                break;
            default:
                assert false;
            }
            return null;
        } else if ("getProxy".equals(method.getName())) {
            return TemplateModelBeanUtil.getProxy(stateNode, args);
        }
        // should not happen
        throw new IllegalArgumentException(
                String.format(
                        "Unknown default TemplateModel method '%s'. "
                                + "Implementation is not available",
                        method.getName()));

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

    private static boolean isTemplateModelProxy(Object proxy) {
        return Proxy.isProxyClass(proxy.getClass())
                && Proxy.getInvocationHandler(
                        proxy) instanceof TemplateModelProxyHandler;
    }

    private static StateNode getStateNodeForProxy(Object proxy) {
        InvocationHandler handler = Proxy.getInvocationHandler(proxy);
        return ((TemplateModelProxyHandler) handler).stateNode;
    }

}
