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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectTools;
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
 */
public record JsCall(Class<?> definitionType, String methodName,
        List<Object> arguments) implements Serializable {

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
     * the given number of arguments, which is the key the generated bundle
     * registers that function under and the only thing the client is told about
     * a call.
     * <p>
     * A hash of the JavaScript, so that the name of the Java that declared it
     * stays on the server. The number of arguments is hashed with it, since it
     * is what the parameters of the generated function are made of, and two
     * methods that declare the same JavaScript for a different number of
     * arguments are two functions.
     *
     * @param expression
     *            the declared JavaScript, not <code>null</code>
     * @param argumentCount
     *            the number of arguments
     * @return the function identifier, not <code>null</code>
     */
    public static String functionId(String expression, int argumentCount) {
        return StringUtil.getHash(argumentCount + ":" + expression,
                StandardCharsets.UTF_8);
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
        JsExpression annotation = resolveMethod()
                .getAnnotation(JsExpression.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Method " + methodName + " of " + definitionType.getName()
                            + " is not annotated with @JsExpression");
        }
        return annotation.value();
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
            return resolveMethod().invoke(implementation, arguments.toArray());
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
    private Method resolveMethod() {
        List<Method> candidates = ReflectTools.getMethodsWithParameterCount(
                definitionType, methodName, arguments.size());
        if (candidates.size() != 1) {
            throw new IllegalStateException("Expected exactly one method named "
                    + methodName + " with " + arguments.size()
                    + " parameters in " + definitionType.getName() + ", found "
                    + candidates.size());
        }
        return candidates.get(0);
    }
}
