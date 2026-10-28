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
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.StringUtil;

/**
 * A call made through {@link Element#executeJs(Class)}: which definition
 * interface, which method of it, and the arguments that were passed.
 * <p>
 * The call is what is scheduled, and what a driver of the client side that can
 * not run JavaScript sees in the pending invocation queue. A browser is sent
 * less than this: the identifier of the function to run and the arguments,
 * never the JavaScript itself, which it looks up in the bundle, and never the
 * interface or the method, which stay on the server. Such a driver can dispatch
 * on the interface and the method, or hand the call to an implementation of the
 * same interface with {@link #invokeOn(Object)} and let Java dispatch it:
 *
 * <pre>
 * if (call.definitionType() == FocusJs.class) {
 *     call.invokeOn(new FocusSimulation(Element.get(pending.getOwner())));
 * }
 * </pre>
 *
 * @param definitionType
 *            the JavaScript definition the call was made on
 * @param methodName
 *            the name of the called method
 * @param arguments
 *            the arguments of the call, in declaration order, any of which may
 *            be <code>null</code>
 * @since 25.4
 */
public record JsCall(Class<?> definitionType, String methodName,
        List<Object> arguments) implements Serializable {

    /**
     * What a JavaScript identifier is made of. Unicode escapes, which an
     * identifier may also contain, are left out: nothing written in Java needs
     * them.
     */
    private static final Pattern IDENTIFIER = Pattern.compile(
            "[\\p{L}\\p{Nl}_$][\\p{L}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}$]*");

    /**
     * What a captured argument is named in the generated function, which a
     * parameter of the inner function would hide.
     */
    private static final Pattern CAPTURE_NAME = Pattern.compile("\\$\\d+");

    /**
     * The words that can not name a parameter of a function in a module, which
     * is what the generated file is, as it runs in strict mode.
     */
    private static final Set<String> RESERVED_WORDS = Set.of("arguments",
            "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "enum", "eval",
            "export", "extends", "false", "finally", "for", "function", "if",
            "implements", "import", "in", "instanceof", "interface", "let",
            "new", "null", "package", "private", "protected", "public",
            "return", "static", "super", "switch", "this", "throw", "true",
            "try", "typeof", "var", "void", "while", "with", "yield");

    /**
     * The methods of each JavaScript definition, each with the identifier of
     * the function it declares. Cached since a call is resolved several times
     * when it is sent, and resolving it otherwise copies the methods of the
     * interface and hashes the declared JavaScript every time. Keyed by any
     * class, since a JavaScript definition is marked by an annotation rather
     * than by a common supertype.
     */
    private static final ReflectionCache<Object, List<DeclaredMethod>> declaredMethods = new ReflectionCache<>(
            JsCall::collectDeclaredMethods);

    /**
     * A method of a JavaScript definition and the identifier of the function it
     * declares, or <code>null</code> for a method that declares no JavaScript.
     */
    private record DeclaredMethod(Method method, @Nullable String functionId) {
    }

    /**
     * Creates a call of the given method of the given JavaScript definition.
     *
     * @param definitionType
     *            the JavaScript definition, not <code>null</code>
     * @param methodName
     *            the name of the called method, not <code>null</code>
     * @param arguments
     *            the arguments of the call, not <code>null</code>
     */
    public JsCall {
        Objects.requireNonNull(definitionType,
                "Definition type cannot be null");
        Objects.requireNonNull(methodName, "Method name cannot be null");
        // Copied rather than List.copyOf, which rejects a null element: an
        // argument may be null, and the client gets it as null
        arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
    }

    /**
     * Gets the identifier of the function that runs the given JavaScript with
     * the given parameters, which is the key the generated bundle registers
     * that function under and the only thing the client is told about a call.
     * <p>
     * A hash of the JavaScript, so that the name of the Java that declared it
     * stays on the server. What the parameters of the generated function are
     * made of is hashed with it, so that two methods declaring the same
     * JavaScript are the same function only when it runs the same way: for the
     * same number of parameters, and with the last one collecting a variable
     * number of arguments in both or in neither.
     *
     * @param expression
     *            the declared JavaScript, not <code>null</code>
     * @param parameterCount
     *            the number of parameters of the declaring method
     * @param variadic
     *            whether its last parameter collects a variable number of
     *            arguments
     * @return the function identifier, not <code>null</code>
     */
    public static String functionId(String expression, int parameterCount,
            boolean variadic) {
        return StringUtil.getHash(
                parameterCount + (variadic ? "*" : "") + ":" + expression,
                StandardCharsets.UTF_8);
    }

    /**
     * Gets the identifier of the function that the given method of a JavaScript
     * definition declares.
     * <p>
     * For a method that runs its JavaScript when it is called, this is
     * {@link #functionId(String, int, boolean)}. For a method that returns a
     * {@link JsSnippet}, the generated function is not run by the call but
     * evaluated later by what the snippet is passed to, and it is generated
     * differently: it is not <code>async</code>, and it may wrap the expression
     * in a function taking the {@link JsCalledWith} parameters. What sets it
     * apart is hashed with the JavaScript, so a snippet and a call that declare
     * the same JavaScript are different functions, and so are two snippets
     * called with different parameters.
     *
     * @param method
     *            a method annotated with {@link JsExpression}, not
     *            <code>null</code>
     * @return the function identifier, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the method is not annotated with {@link JsExpression}, or
     *             declares parameter names that can not be generated
     */
    public static String functionId(Method method) {
        JsExpression annotation = method.getAnnotation(JsExpression.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Method " + method.getName()
                    + " of " + method.getDeclaringClass().getName()
                    + " is not annotated with @JsExpression");
        }
        String expression = annotation.value();
        int parameterCount = method.getParameterCount();
        boolean variadic = method.isVarArgs();
        if (!JsSnippet.class.isAssignableFrom(method.getReturnType())) {
            return functionId(expression, parameterCount, variadic);
        }
        List<String> calledWith = getCalledWithNames(method);
        // A call is hashed from what starts with the parameter count, so a
        // snippet starting with anything else never shares its hash input
        String kind = (calledWith == null ? ""
                : "(" + String.join(",", calledWith) + ")") + "=>";
        return StringUtil.getHash(kind + parameterCount + (variadic ? "*" : "")
                + ":" + expression, StandardCharsets.UTF_8);
    }

    /**
     * Gets the names of the parameters that the function a snippet evaluates to
     * is called with, as declared by {@link JsCalledWith} on the given method.
     * <p>
     * The names become parameters of the generated function, so each of them is
     * checked to be one that a function in a JavaScript module can declare and
     * that does not hide one of the captured arguments.
     *
     * @param method
     *            a method of a JavaScript definition, not <code>null</code>
     * @return the parameter names in the order they are passed, or
     *         <code>null</code> when the method declares none and no function
     *         is wrapped around the expression
     * @throws IllegalArgumentException
     *             if a name is not a JavaScript identifier, is a reserved word,
     *             names a captured argument or is declared twice
     */
    public static @Nullable List<String> getCalledWithNames(Method method) {
        JsCalledWith annotation = method.getAnnotation(JsCalledWith.class);
        if (annotation == null) {
            return null;
        }
        List<String> names = List.of(annotation.value());
        for (String name : names) {
            if (!IDENTIFIER.matcher(name).matches()
                    || RESERVED_WORDS.contains(name)
                    || CAPTURE_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Method " + method.getName()
                        + " of " + method.getDeclaringClass().getName()
                        + " declares '" + name
                        + "' in @JsCalledWith, which can not name a parameter."
                        + " Use a JavaScript identifier that is not a reserved"
                        + " word and not $0, $1, ..., which name the captured"
                        + " arguments");
            }
        }
        if (Set.copyOf(names).size() != names.size()) {
            throw new IllegalArgumentException("Method " + method.getName()
                    + " of " + method.getDeclaringClass().getName()
                    + " declares a name more than once in @JsCalledWith: "
                    + names);
        }
        return names;
    }

    /**
     * Gets the identifier of the function that runs this call, which is what
     * the client looks the JavaScript up in the bundle by.
     *
     * @return the function identifier, not <code>null</code>
     */
    public String getFunctionId() {
        String functionId = resolveMethod().functionId();
        if (functionId == null) {
            throw notDeclaredException();
        }
        return functionId;
    }

    /**
     * Gets the names of the parameters that the function a snippet call
     * declares is called with.
     *
     * @return the parameter names, or <code>null</code> when the called method
     *         declares none
     * @see #getCalledWithNames(Method)
     */
    public @Nullable List<String> getCalledWithNames() {
        return getCalledWithNames(resolveMethod().method());
    }

    /**
     * Whether the called method collects a variable number of arguments into
     * its last parameter.
     * <p>
     * The generated function collects them into a rest parameter, which does
     * not count towards the length of the function, so the client is told how
     * many arguments a call of such a function carries rather than reading it
     * off the function.
     *
     * @return <code>true</code> when the called method is variadic
     */
    public boolean isVariadic() {
        return resolveMethod().method().isVarArgs();
    }

    /**
     * Gets the arguments of this call as the client receives them, which
     * spreads the array that a variadic call collects its trailing arguments
     * into.
     * <p>
     * {@link #arguments()} holds what Java passed, where a variadic tail is a
     * single array argument. The browser gets each of its values on its own, so
     * that an element, a return channel or a function among them is sent as the
     * reference it is rather than as part of a nested array.
     *
     * @return the arguments as the client receives them, not <code>null</code>
     */
    public List<Object> flattenArguments() {
        if (arguments.isEmpty() || !isVariadic()) {
            return arguments;
        }
        List<Object> flattened = new ArrayList<>(
                arguments.subList(0, arguments.size() - 1));
        Object tail = arguments.get(arguments.size() - 1);
        // A call written as method(name, (Object[]) null) passes no trailing
        // arguments rather than one null argument, as Java reads it
        if (tail != null) {
            // Read through reflection rather than as an Object[], so that a
            // method declaring a primitive tail - int... for one - hands the
            // client its boxed values instead of failing on the array
            int length = Array.getLength(tail);
            for (int index = 0; index < length; index++) {
                flattened.add(Array.get(tail, index));
            }
        }
        return Collections.unmodifiableList(flattened);
    }

    /**
     * Gets what this call is sent with: its arguments as the client receives
     * them, and then the element to run the function on, which the client
     * applies the function to.
     * <p>
     * The element a call was made on goes into that last place, and a call made
     * on nothing in particular - page JavaScript, which works on globals - puts
     * <code>null</code> there, so a client reads the two the same way.
     *
     * @param runOn
     *            the element to run the function on, or <code>null</code> for
     *            page JavaScript, which runs on nothing in particular
     * @return the parameters of the call, not <code>null</code>
     */
    public Object[] parametersFor(@Nullable Element runOn) {
        List<Object> parameters = new ArrayList<>(flattenArguments());
        parameters.add(runOn);
        return parameters.toArray();
    }

    /**
     * Gets the JavaScript that this call runs in a browser, as declared by
     * {@link JsExpression} on the called method.
     * <p>
     * The expression is not sent to the client, which runs the function that
     * the build generated from the same declaration. It is available here for
     * the server side, for instance for a test that asserts what a browser
     * would run.
     *
     * @return the JavaScript expression, not <code>null</code>
     */
    public String getExpression() {
        return expressionOf(resolveMethod().method());
    }

    private String expressionOf(Method method) {
        JsExpression annotation = method.getAnnotation(JsExpression.class);
        if (annotation == null) {
            throw notDeclaredException();
        }
        return annotation.value();
    }

    private IllegalStateException notDeclaredException() {
        return new IllegalStateException(
                "Method " + methodName + " of " + definitionType.getName()
                        + " is not annotated with @JsExpression");
    }

    /**
     * Runs this call on an implementation of the JavaScript definition, which
     * is how a driver of the client side reproduces it without running the
     * JavaScript.
     *
     * @param implementation
     *            an implementation of {@link #definitionType()}, not
     *            <code>null</code>
     * @return the value returned by the implementation, or <code>null</code>
     *         for a void method
     * @throws IllegalArgumentException
     *             if the implementation does not implement
     *             {@link #definitionType()}
     */
    public Object invokeOn(Object implementation) {
        if (!definitionType.isInstance(implementation)) {
            throw new IllegalArgumentException(
                    implementation.getClass().getName() + " does not implement "
                            + definitionType.getName());
        }
        try {
            return resolveMethod().method().invoke(implementation,
                    arguments.toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable cause = e instanceof InvocationTargetException
                    ? e.getCause()
                    : e;
            // What the implementation threw is what the caller wants to see
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(
                    "Could not run " + methodName + " on " + implementation,
                    cause);
        }
    }

    /**
     * Finds the called method by name and argument count. Overloads that differ
     * only in parameter types are not distinguishable this way, which is a
     * limitation of the prototype rather than of the idea.
     */
    private DeclaredMethod resolveMethod() {
        List<DeclaredMethod> candidates = declaredMethods.get(definitionType)
                .stream()
                .filter(declared -> declared.method().getName()
                        .equals(methodName)
                        && declared.method().getParameterCount() == arguments
                                .size())
                .toList();
        if (candidates.size() != 1) {
            throw new IllegalStateException("Expected exactly one method named "
                    + methodName + " with " + arguments.size()
                    + " parameters in " + definitionType.getName() + ", found "
                    + candidates.size());
        }
        return candidates.get(0);
    }

    private static List<DeclaredMethod> collectDeclaredMethods(
            Class<?> definitionType) {
        List<DeclaredMethod> declared = new ArrayList<>();
        for (Method method : definitionType.getMethods()) {
            declared.add(new DeclaredMethod(method,
                    method.isAnnotationPresent(JsExpression.class)
                            ? functionId(method)
                            : null));
        }
        return List.copyOf(declared);
    }
}
