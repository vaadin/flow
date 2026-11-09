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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.type.TypeFactory;

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.js.internal.JsWithExecutionContext;

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

    /**
     * The type that every method of a checked JavaScript definition returning
     * {@link JavaScriptResult} converts its value to, so that the generic
     * return type is resolved once per definition rather than on every call.
     * Cleared before a redefined class is hotswapped.
     */
    private static final ReflectionCache<Object, Map<Method, JavaType>> resultTypes = ReflectionCache
            .clearedBeforeHotswap(JsDefinitionProxy::collectResultTypes);

    private static final String RETURN_TYPES = "An interface declares either"
            + " JavaScript to run, with methods returning void,"
            + " JavaScriptResult or PendingJavaScriptResult, or snippets, with"
            + " methods returning JsFunction, JsSnippet, DomEventFilter or"
            + " DomEventData, but not both";

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
            String reason = names.isEmpty() ? " declares no snippets"
                    : (" declares JavaScript that runs when "
                            + String.join(", ", names)
                            + " is called, not snippets. Get its implementation"
                            + " from Element.executeJs(Class)");
            throw new IllegalArgumentException(
                    definitionType.getName() + reason);
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
     *             {@link JsDefinition}, is annotated with {@link JsCalledWith},
     *             or has a method that does not fit what the definition
     *             declares
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
        if (definitionType.isAnnotationPresent(JsCalledWith.class)) {
            // Only the methods and the snippet types they return are read
            throw new IllegalArgumentException(definitionType.getName()
                    + " is annotated with @JsCalledWith, which applies to a"
                    + " method that returns a JsSnippet or to a snippet type,"
                    + " not to a JavaScript definition. Annotate each method"
                    + " instead");
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
     * the JavaScript it runs, and returns either nothing or the result of
     * running it, typed or pending, or, for a definition of snippets, every one
     * of them returns the snippet it declares. An interface that declares
     * JavaScript declares nothing else, so a method that is implemented in Java
     * - a default or a static one - is refused rather than left aside.
     */
    private static void checkMethods(Class<?> definitionType,
            boolean snippets) {
        List<String> undeclared = new ArrayList<>();
        List<String> unanswerable = new ArrayList<>();
        List<String> untyped = new ArrayList<>();
        List<String> inJava = new ArrayList<>();
        // Not the shared mapper's factory, whose cache would keep the classes
        // of the project that the build checks reachable after the build
        TypeFactory types = TypeFactory.createDefaultInstance();
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
            if (!snippets && method.getReturnType() == JavaScriptResult.class
                    && typedResultType(method, definitionType, types) == null) {
                untyped.add(method.getName());
            } else if (snippets ? !returnsSnippet(method)
                    : !returnsInvocationResult(method)) {
                unanswerable.add(method.getName());
            } else if (snippets) {
                if (method.isAnnotationPresent(JsWithExecutionContext.class)) {
                    throw new IllegalArgumentException(definitionType.getName()
                            + " has " + method.getName()
                            + " annotated with @JsWithExecutionContext, which"
                            + " only applies to a method that runs JavaScript"
                            + " when it is called");
                }
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
        if (!untyped.isEmpty()) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " has " + String.join(", ", untyped)
                    + " returning JavaScriptResult without a type to convert"
                    + " the result to. Declare a concrete type argument, such"
                    + " as JavaScriptResult<Integer>, or return"
                    + " PendingJavaScriptResult for the JSON value");
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
        // Also true for JavaScriptResult, as a supertype of the pending result,
        // whose type argument checkMethods checks before getting here
        return returnType == void.class
                || returnType.isAssignableFrom(PendingJavaScriptResult.class);
    }

    private static Map<Method, JavaType> collectResultTypes(
            Class<?> definitionType) {
        TypeFactory typeFactory = JacksonUtils.getMapper().getTypeFactory();
        Map<Method, JavaType> types = new HashMap<>();
        for (Method method : definitionType.getMethods()) {
            if (method.getReturnType() == JavaScriptResult.class) {
                JavaType type = typedResultType(method, definitionType,
                        typeFactory);
                if (type != null) {
                    types.put(method, type);
                }
            }
        }
        return Map.copyOf(types);
    }

    /**
     * Gets the type to convert the value of a method returning
     * {@link JavaScriptResult} to, or <code>null</code> if its type argument is
     * raw, a wildcard or refers to a type variable that the definition does not
     * bind to a type, none of which tells what to convert the result to.
     */
    private static JavaType typedResultType(Method method,
            Class<?> definitionType, TypeFactory types) {
        if (method
                .getGenericReturnType() instanceof ParameterizedType parameterized) {
            Type argument = parameterized.getActualTypeArguments()[0];
            return argument instanceof WildcardType ? null
                    : resolve(argument, definitionType, types);
        }
        return null;
    }

    /**
     * Resolves a type as seen from the given JavaScript definition, or answers
     * <code>null</code> if it refers to a type variable that the definition
     * does not bind to a type, such as one of its own, of a method, or of an
     * interface it inherits as a raw type. A wildcard within a type argument
     * resolves to its upper bound, as Jackson does.
     */
    private static JavaType resolve(Type type, Class<?> definitionType,
            TypeFactory types) {
        return switch (type) {
        case Class<?> plain -> types.constructType(plain);
        case TypeVariable<?> variable -> {
            if (!(variable.getGenericDeclaration() instanceof Class<?>)) {
                yield null;
            }
            @SuppressWarnings("unchecked")
            Type bound = ReflectTools.getTypeParameter(definitionType,
                    (TypeVariable<Class<?>>) variable);
            // A variable nested in the bound type, as in List<T>, belongs to
            // an interface between the definition and the one declaring this
            // variable, so it is resolved the same way
            yield bound == null ? null : resolve(bound, definitionType, types);
        }
        case ParameterizedType parameterized -> {
            Type[] arguments = parameterized.getActualTypeArguments();
            JavaType[] resolved = new JavaType[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                Type argument = arguments[i] instanceof WildcardType wildcard
                        ? wildcard.getUpperBounds()[0]
                        : arguments[i];
                resolved[i] = resolve(argument, definitionType, types);
                if (resolved[i] == null) {
                    yield null;
                }
            }
            yield types.constructParametricType(
                    ReflectTools.erase(parameterized), resolved);
        }
        case GenericArrayType array -> {
            JavaType component = resolve(array.getGenericComponentType(),
                    definitionType, types);
            yield component == null ? null
                    : types.constructArrayType(component);
        }
        default -> null;
        };
    }

    /**
     * Checks whether the method returns a snippet type that this class can
     * create.
     */
    private static boolean returnsSnippet(Method method) {
        return snippetFactory(method.getReturnType()) != null;
    }

    /**
     * Gets the factory that creates a snippet for a method with the given
     * return type. The factory gets the call and the generic return type of the
     * method. It creates one of these:
     * <ul>
     * <li>a {@link JsFunction}, if the return type allows it, or</li>
     * <li>a {@link DomEventFilter} or a {@link DomEventData}, which a DOM event
     * listener runs.</li>
     * </ul>
     *
     * @return the factory, or <code>null</code> if the type is not a snippet
     *         type that this class can create
     */
    private static BiFunction<JsCall, Type, JsSnippet> snippetFactory(
            Class<?> returnType) {
        if (returnType == DomEventFilter.class) {
            return (call, genericType) -> DomEventFilter.ofDeclaredCall(call);
        } else if (returnType == DomEventData.class) {
            return DomEventData::ofDeclaredCall;
        } else if (JsSnippet.class.isAssignableFrom(returnType)
                && returnType.isAssignableFrom(JsFunction.class)) {
            return (call, genericType) -> JsFunction.ofDeclaredCall(call);
        }
        return null;
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
            List<Object> arguments = args == null ? List.of()
                    : Arrays.asList(args);
            PendingJavaScriptResult result = runner.apply(
                    new JsCall(definitionType, method.getName(), arguments));
            Class<?> returnType = method.getReturnType();
            if (returnType == JavaScriptResult.class) {
                JavaType valueType = resultTypes.get(definitionType)
                        .get(method);
                // Not wrapped, because the pending result already is a
                // JavaScriptResult<JsonNode> and hands a JavaScript null over
                // as a NullNode, like its own then(Consumer<JsonNode>).
                // Decoding turns null into a Java null instead, which is right
                // for other node types such as ObjectNode, as with
                // then(Class, Consumer), but would change what JsonNode gets.
                return valueType.hasRawClass(JsonNode.class) ? result
                        : new TypedJavaScriptResult<>(result, valueType);
            }
            return returnType == void.class ? null : result;
        }
    }

    /**
     * A {@link JavaScriptResult} that converts the JSON value of a pending
     * result to the type that a method of a JavaScript definition declares. It
     * adds a handler to the pending result only when a handler is added to it,
     * so a call whose result nobody reads has no round trip for the result.
     */
    private record TypedJavaScriptResult<T>(PendingJavaScriptResult pending,
            JavaType valueType) implements JavaScriptResult<T> {

        @Override
        public boolean cancelExecution() {
            return pending.cancelExecution();
        }

        @Override
        public boolean isSentToBrowser() {
            return pending.isSentToBrowser();
        }

        @Override
        public void then(SerializableConsumer<T> resultHandler,
                SerializableConsumer<String> errorHandler) {
            if (resultHandler == null) {
                throw new IllegalArgumentException(
                        "Result handler cannot be null");
            }
            pending.then(json -> {
                T value;
                try {
                    value = JacksonCodec.decodeAs(json, valueType);
                } catch (IllegalArgumentException e) {
                    // Not thrown, since the pending result chains the handlers
                    // added to it, so throwing would also skip any added after
                    // this one, such as one completing a future
                    if (errorHandler != null) {
                        errorHandler.accept(e.getMessage());
                    } else {
                        LoggerFactory.getLogger(JsDefinitionProxy.class).warn(
                                "Ignored a JavaScript result that can not be"
                                        + " converted: {}",
                                e.getMessage());
                    }
                    return;
                }
                resultHandler.accept(value);
            }, errorHandler);
        }
    }

    /**
     * Answers a call of a JavaScript definition with the snippet that the call
     * declares. Nothing runs on the server.
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
            return snippetFactory(method.getReturnType()).apply(
                    new JsCall(definitionType, method.getName(), arguments),
                    method.getGenericReturnType());
        }
    }
}
