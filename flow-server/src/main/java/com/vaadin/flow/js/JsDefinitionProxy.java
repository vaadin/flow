/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.js;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Hands out implementations of {@link JsDefinition} interfaces, which turn a
 * call of a declared method into a {@link JsCall} and give it to whoever runs
 * it.
 * <p>
 * The interface is checked here rather than when a method is called, so one
 * that can not work says so when the implementation is handed out.
 * <p>
 * For internal use only. Call {@link Element#executeJs(Class)}, which runs the
 * call on the element the implementation was obtained from.
 * 
 * @since 25.4
 */
public final class JsDefinitionProxy {

    private JsDefinitionProxy() {
        // Only static members
    }

    /**
     * Creates an implementation of the given JavaScript definition, which hands
     * every call of it to the given function and answers with what the function
     * answers, for a method that declares a result.
     *
     * @param <T>
     *            the JavaScript definition type
     * @param definitionType
     *            the JavaScript definition, not <code>null</code>
     * @param runner
     *            what runs a call of the interface, not <code>null</code>
     * @return an implementation of the interface, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the type is not an interface, is not annotated with
     *             {@link JsDefinition}, or has a method that can not be
     *             answered
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> definitionType,
            SerializableFunction<JsCall, PendingJavaScriptResult> runner) {
        Objects.requireNonNull(definitionType,
                "Definition type cannot be null");
        Objects.requireNonNull(runner, "Runner cannot be null");
        if (!definitionType.isInterface()) {
            throw new IllegalArgumentException(
                    definitionType.getName() + " is not an interface");
        }
        if (!definitionType.isAnnotationPresent(JsDefinition.class)) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " is not annotated with @JsDefinition, so the build does not"
                    + " collect its JavaScript into the bundle");
        }
        checkMethods(definitionType);
        return (T) Proxy.newProxyInstance(definitionType.getClassLoader(),
                new Class<?>[] { definitionType },
                new JsDefinitionHandler(runner, definitionType));
    }

    /**
     * Checks the methods of a JavaScript definition: every one of them declares
     * the JavaScript it runs, and returns either nothing or the pending result
     * of running it. An interface that declares JavaScript declares nothing
     * else, so a method that is implemented in Java - a default or a static one
     * - is refused rather than left aside.
     */
    private static void checkMethods(Class<?> definitionType) {
        List<String> undeclared = new ArrayList<>();
        List<String> unanswerable = new ArrayList<>();
        List<String> inJava = new ArrayList<>();
        for (Method method : definitionType.getMethods()) {
            if (method.isDefault()
                    || Modifier.isStatic(method.getModifiers())) {
                inJava.add(method.getName());
                continue;
            }
            if (!method.isAnnotationPresent(JsExpression.class)) {
                // What it returns is beside the point until it declares
                // something to return it from, and the first list that has
                // anything in it is the one that is reported, so this changes
                // what the lists hold rather than what is said
                undeclared.add(method.getName());
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType != void.class && !returnType
                    .isAssignableFrom(PendingJavaScriptResult.class)) {
                unanswerable.add(method.getName());
            }
        }
        if (!undeclared.isEmpty()) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " declares no JavaScript to run for "
                    + String.join(", ", undeclared)
                    + ". Annotate every method with @JsExpression");
        }
        if (!unanswerable.isEmpty()) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " has " + String.join(", ", unanswerable)
                    + " returning something that can not be answered with."
                    + " A method returns void or PendingJavaScriptResult");
        }
        if (!inJava.isEmpty()) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " has " + String.join(", ", inJava)
                    + " implemented in Java, which is not what an interface"
                    + " that declares JavaScript is for. Annotate every method"
                    + " with @JsExpression, and compose the calls in the class"
                    + " that makes them");
        }
    }

    /**
     * Turns a call of a JavaScript definition into a {@link JsCall} and runs it
     * through the function it was created with.
     */
    private record JsDefinitionHandler(
            SerializableFunction<JsCall, PendingJavaScriptResult> runner,
            Class<?> definitionType)
            implements
                InvocationHandler,
                Serializable {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            boolean returnsResult = method.getReturnType()
                    .isAssignableFrom(PendingJavaScriptResult.class);
            List<Object> arguments = args == null ? List.of()
                    : Arrays.asList(args);
            PendingJavaScriptResult result = runner.apply(
                    new JsCall(definitionType, method.getName(), arguments));
            return returnsResult ? result : null;
        }
    }
}
