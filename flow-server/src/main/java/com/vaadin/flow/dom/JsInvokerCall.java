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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A call made through {@link Element#getJsInvoker(Class)}: which invoker
 * interface, which method of it, and the arguments that were passed.
 * <p>
 * This is the {@link JsCommand} that the invoker schedules, so the call is what
 * a driver of the client side sees in the pending JavaScript queue. It can
 * dispatch on the interface and the method name, or hand the call to an
 * implementation of the same interface with {@link #invokeOn(Object)} and let
 * Java do the dispatching:
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
        List<Object> arguments) implements JsCommand {

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

    @Override
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

    @Override
    public List<Object> getParameters() {
        return arguments;
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
