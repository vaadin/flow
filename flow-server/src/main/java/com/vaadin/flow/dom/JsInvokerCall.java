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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A call made through {@link Element#getJsInvoker(Class)}: which invoker
 * interface, which method of it, and the arguments that were passed.
 * <p>
 * The call is what the client receives — the interface, the method and the
 * arguments, never the JavaScript itself, which the client looks up in the
 * bundle. It is also what a driver of the client side that can not run
 * JavaScript sees in the pending invocation queue. Such a driver can dispatch
 * on the interface and the method, or hand the call to an implementation of the
 * same interface with {@link #invokeOn(Object)} and let Java dispatch it:
 *
 * <pre>
 * if (call.invokerType() == FocusJs.class) {
 *     call.invokeOn(new FocusSimulation(Element.get(pending.getOwner())));
 * }
 * </pre>
 *
 * @param invokerType
 *            the invoker interface the call was made on
 * @param methodName
 *            the name of the called method
 * @param arguments
 *            the arguments of the call, in declaration order
 */
public record JsInvokerCall(Class<?> invokerType, String methodName,
        List<Object> arguments) implements Serializable {

    /**
     * Creates a call of the given method of the given invoker interface.
     *
     * @param invokerType
     *            the invoker interface, not <code>null</code>
     * @param methodName
     *            the name of the called method, not <code>null</code>
     * @param arguments
     *            the arguments of the call, not <code>null</code>
     */
    public JsInvokerCall {
        Objects.requireNonNull(invokerType, "Invoker type cannot be null");
        Objects.requireNonNull(methodName, "Method name cannot be null");
        arguments = List.copyOf(arguments);
    }

    /**
     * Gets the identifier of the invoker interface, which is the key the
     * generated bundle registers its functions under.
     *
     * @return the invoker identifier, not <code>null</code>
     */
    public String getInvokerId() {
        return invokerType.getName();
    }

    /**
     * Gets the identifier of the called method within its invoker, which is the
     * method name and the number of arguments, so that overloads stay apart.
     *
     * @return the method identifier, not <code>null</code>
     */
    public String getMethodId() {
        return methodId(methodName, arguments.size());
    }

    /**
     * Gets the identifier of a method with the given name and number of
     * arguments.
     *
     * @param methodName
     *            the method name, not <code>null</code>
     * @param argumentCount
     *            the number of arguments
     * @return the method identifier, not <code>null</code>
     */
    public static String methodId(String methodName, int argumentCount) {
        return methodName + "/" + argumentCount;
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
                    "Method " + methodName + " of " + invokerType.getName()
                            + " is not annotated with @JsExpression");
        }
        return annotation.value();
    }

    /**
     * Runs this call on an implementation of the invoker interface, which is
     * how a driver of the client side reproduces it without running the
     * JavaScript.
     *
     * @param implementation
     *            an implementation of {@link #invokerType()}, not
     *            <code>null</code>
     * @return the value returned by the implementation, or <code>null</code>
     *         for a void method
     */
    public Object invokeOn(Object implementation) {
        if (!invokerType.isInstance(implementation)) {
            throw new IllegalArgumentException(
                    implementation.getClass().getName() + " does not implement "
                            + invokerType.getName());
        }
        try {
            return resolveMethod().invoke(implementation, arguments.toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    "Could not run " + methodName + " on " + implementation, e);
        }
    }

    /**
     * Finds the called method by name and argument count. Overloads that differ
     * only in parameter types are not distinguishable this way, which is a
     * limitation of the prototype rather than of the idea.
     */
    private Method resolveMethod() {
        List<Method> candidates = Arrays.stream(invokerType.getMethods())
                .filter(method -> method.getName().equals(methodName)
                        && method.getParameterCount() == arguments.size())
                .toList();
        if (candidates.size() != 1) {
            throw new IllegalStateException("Expected exactly one method named "
                    + methodName + " with " + arguments.size()
                    + " parameters in " + invokerType.getName() + ", found "
                    + candidates.size());
        }
        return candidates.get(0);
    }
}
