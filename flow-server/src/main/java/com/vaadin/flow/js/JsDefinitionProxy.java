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
import java.util.stream.Stream;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.ReflectionCache;

/**
 * Hands out implementations of {@link JsDefinition} interfaces, which turn a
 * call of a declared method into a {@link JsCall} and give it to whoever runs
 * it.
 * <p>
 * The interface is checked here rather than when a method is called, so one
 * that can not work says so when the implementation is handed out.
 * <p>
 * For internal use only. Call {@link Element#executeJs(Class)}, which runs the
 * call on the element the implementation was obtained from, or
 * {@link JsSnippets#of(Class)} for an interface that declares snippets.
 * 
 * @since 25.4
 */
public final class JsDefinitionProxy {

    /**
     * What a JavaScript definition declares.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     */
    public enum Kind {
        /**
         * JavaScript that runs when a method is called, through
         * {@link Element#executeJs(Class)}.
         */
        CALLS,
        /**
         * {@link JsSnippet Snippets}, which a method answers with, through
         * {@link JsSnippets#of(Class)}.
         */
        SNIPPETS
    }

    /**
     * The JavaScript definitions that have been checked, with what they
     * declare, so that an interface is checked once rather than every time an
     * implementation of it is handed out. A definition that fails the check is
     * not kept, so it fails again on the next attempt. Cleared before a
     * redefined class is hotswapped, so a changed interface is checked again.
     * Keyed by any class, since a JavaScript definition is marked by an
     * annotation rather than by a common supertype.
     */
    private static final ReflectionCache<Object, Kind> checkedDefinitions = ReflectionCache
            .clearedBeforeHotswap(JsDefinitionProxy::checkDefinition);

    private static final String RETURN_TYPES = "An interface declares either"
            + " JavaScript to run, with methods returning void or"
            + " PendingJavaScriptResult, or snippets, with methods returning"
            + " JsFunction or JsSnippet, but not both";

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
        if (checkedDefinitions.get(definitionType) != Kind.CALLS) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " declares snippets, which are not run on an element."
                    + " Get its implementation from JsSnippets.of");
        }
        return (T) Proxy.newProxyInstance(definitionType.getClassLoader(),
                new Class<?>[] { definitionType },
                new JsDefinitionHandler(runner, definitionType));
    }

    /**
     * Creates an implementation of the given JavaScript definition whose
     * methods declare {@link JsSnippet snippets}, which answers every call of
     * it with the snippet that the call declares rather than running anything.
     *
     * @param <T>
     *            the JavaScript definition type
     * @param definitionType
     *            the JavaScript definition, not <code>null</code>
     * @return an implementation of the interface, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the type is not an interface, is not annotated with
     *             {@link JsDefinition}, or has a method that does not declare a
     *             snippet
     */
    @SuppressWarnings("unchecked")
    public static <T> T createSnippets(Class<T> definitionType) {
        Objects.requireNonNull(definitionType,
                "Definition type cannot be null");
        if (checkedDefinitions.get(definitionType) != Kind.SNIPPETS) {
            List<String> names = collectDeclaredNames(definitionType);
            throw new IllegalArgumentException(definitionType.getName()
                    + (names.isEmpty() ? " declares no snippets"
                            : " declares JavaScript that runs when "
                                    + String.join(", ", names)
                                    + " is called, not snippets. Get its"
                                    + " implementation from"
                                    + " Element.executeJs(Class)"));
        }
        return (T) Proxy.newProxyInstance(definitionType.getClassLoader(),
                new Class<?>[] { definitionType },
                new JsSnippetHandler(definitionType));
    }

    /**
     * Checks that the given type is a JavaScript definition that can work, and
     * tells what it declares. The same rules apply to the build, which
     * generates the JavaScript of a definition only when it passes, and to the
     * implementations handed out at runtime.
     * <p>
     * Not cached, so that the build, which checks every definition once, holds
     * no reference to the classes of the project it builds.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param definitionType
     *            the type to check, not <code>null</code>
     * @return what the definition declares, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the type is not an interface, is not annotated with
     *             {@link JsDefinition}, or has a method that does not fit what
     *             the definition declares
     */
    public static Kind checkDefinition(Class<?> definitionType) {
        if (!definitionType.isInterface()) {
            throw new IllegalArgumentException(
                    definitionType.getName() + " is not an interface");
        }
        if (!definitionType.isAnnotationPresent(JsDefinition.class)) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " is not annotated with @JsDefinition, so the build does not"
                    + " collect its JavaScript into the bundle");
        }
        // One snippet makes it a definition of snippets, so that a method
        // running JavaScript beside it is reported as the odd one out
        boolean snippets = Stream.of(definitionType.getMethods()).anyMatch(
                method -> method.isAnnotationPresent(JsExpression.class)
                        && JsSnippet.class
                                .isAssignableFrom(method.getReturnType()));
        checkMethods(definitionType, snippets);
        return snippets ? Kind.SNIPPETS : Kind.CALLS;
    }

    private static List<String> collectDeclaredNames(Class<?> definitionType) {
        return Stream.of(definitionType.getMethods()).map(Method::getName)
                .sorted().toList();
    }

    /**
     * Checks the methods of a JavaScript definition: every one of them declares
     * the JavaScript it runs, and returns either nothing or the pending result
     * of running it, or, for a definition of snippets, every one of them
     * returns the snippet it declares. An interface that declares JavaScript
     * declares nothing else, so a method that is implemented in Java - a
     * default or a static one - is refused rather than left aside.
     */
    private static void checkMethods(Class<?> definitionType,
            boolean snippets) {
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
            if (snippets ? !returnsFunction(method)
                    : !returnsInvocationResult(method)) {
                unanswerable.add(method.getName());
            } else if (snippets) {
                // Throws for names the build can not generate a function with
                JsCall.getCalledWithNames(method);
            } else if (method.isAnnotationPresent(JsCalledWith.class)) {
                throw new IllegalArgumentException(definitionType.getName()
                        + " has " + method.getName()
                        + " annotated with @JsCalledWith, which only applies to"
                        + " a method that returns a JsSnippet");
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
                    + " returning something that can not be answered with. "
                    + RETURN_TYPES);
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

    private static boolean returnsInvocationResult(Method method) {
        Class<?> returnType = method.getReturnType();
        return returnType == void.class
                || returnType.isAssignableFrom(PendingJavaScriptResult.class);
    }

    /**
     * Whether the method returns a snippet type that a {@link JsFunction} is,
     * which is the only kind of snippet there is to answer with.
     */
    private static boolean returnsFunction(Method method) {
        Class<?> returnType = method.getReturnType();
        return JsSnippet.class.isAssignableFrom(returnType)
                && returnType.isAssignableFrom(JsFunction.class);
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

    /**
     * Answers a call of a JavaScript definition with the function that the call
     * declares, which runs nothing on the server.
     */
    private record JsSnippetHandler(Class<?> definitionType)
            implements
                InvocationHandler,
                Serializable {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            List<Object> arguments = args == null ? List.of()
                    : Arrays.asList(args);
            return JsFunction.ofDeclaredCall(
                    new JsCall(definitionType, method.getName(), arguments));
        }
    }
}
