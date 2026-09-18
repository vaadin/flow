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
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Creates the invokers that the entry points hand out, so that an invoker
 * behaves the same whichever of them it came from.
 * <p>
 * An entry point supplies what it alone knows: how a call it produced is
 * scheduled. Everything else - which interfaces are usable, what a method may
 * return, and turning a call on the interface into a {@link JsInvokerCall} - is
 * the same everywhere and lives here.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class JsInvokers implements Serializable {

    private JsInvokers() {
    }

    /**
     * Creates an invoker for the given interface, scheduling every call it
     * receives with the given scheduler.
     *
     * @param <T>
     *            the invoker interface type
     * @param invokerType
     *            the invoker interface, annotated with {@link JsInvoker}, not
     *            <code>null</code>
     * @param scheduler
     *            what sends a call to the client, not <code>null</code>
     * @return an invoker for the interface, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the type is not an interface, or is not annotated with
     *             {@link JsInvoker} and therefore has no JavaScript in the
     *             bundle
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> invokerType,
            SerializableFunction<JsInvokerCall, PendingJavaScriptResult> scheduler) {
        Objects.requireNonNull(invokerType, "Invoker type cannot be null");
        Objects.requireNonNull(scheduler, "Scheduler cannot be null");
        if (!invokerType.isInterface()) {
            throw new IllegalArgumentException(
                    invokerType.getName() + " is not an interface");
        }
        if (!invokerType.isAnnotationPresent(JsInvoker.class)) {
            throw new IllegalArgumentException(invokerType.getName()
                    + " is not annotated with @JsInvoker, so the build does not"
                    + " collect its JavaScript into the bundle");
        }
        return (T) Proxy.newProxyInstance(invokerType.getClassLoader(),
                new Class<?>[] { invokerType },
                new JsInvokerHandler(invokerType, scheduler));
    }

    /**
     * Turns a call on an invoker interface into a scheduled invocation that
     * carries the call.
     */
    private record JsInvokerHandler(Class<?> invokerType,
            SerializableFunction<JsInvokerCall, PendingJavaScriptResult> scheduler)
            implements
                InvocationHandler,
                Serializable {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            Class<?> returnType = method.getReturnType();
            boolean returnsResult = returnType
                    .isAssignableFrom(PendingJavaScriptResult.class);
            // Checked before scheduling, so that a method the invoker can not
            // answer does not run in the browser either
            if (returnType != void.class && !returnsResult) {
                throw new IllegalStateException("Method " + method.getName()
                        + " of " + invokerType.getName()
                        + " must return void or PendingJavaScriptResult");
            }
            List<Object> arguments = args == null ? List.of()
                    : Arrays.asList(args);
            PendingJavaScriptResult result = scheduler.apply(new JsInvokerCall(
                    invokerType, method.getName(), arguments));
            return returnsResult ? result : null;
        }
    }
}
