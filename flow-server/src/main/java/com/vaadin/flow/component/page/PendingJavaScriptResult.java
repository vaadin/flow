/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.page;

import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.component.page.Page.ExecutionCanceler;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JsonCodec;

import elemental.json.JsonValue;

/**
 * A pending result from a JavaScript snippet sent to the browser for
 * evaluation.
 * <p>
 * If any of the <code>then</code> or <code>toCompletableFuture</code> methods
 * have been invoked before the snippet is sent to the browser, then the there
 * will be an additional round trip for sending the results of the evaluation
 * back to any registered handler. If the JavaScript execution returns a
 * <code>Promise</code>, then the result will be sent to the server only when it
 * is resolved.
 * <p>
 * It is not possible to add handlers after the invocation has been sent to the
 * browser since the original result would in that case already be discarded.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
@SuppressWarnings("deprecation")
public interface PendingJavaScriptResult extends ExecutionCanceler {

    /**
     * Exception used when a {@link CompletableFuture} returned from
     * {@link PendingJavaScriptResult} is completed exceptionally because of a
     * client-side error.
     */
    public class JavaScriptException extends RuntimeException {
        /**
         * Creates a new JavaScript exception with the given message.
         *
         * @param message
         *            the exception message, not <code>null</code>
         */
        public JavaScriptException(String message) {
            super(message);
        }
    }

    // Explicitly defined to clear the inherited deprecation marker
    @Override
    boolean cancelExecution();

    /**
     * Checks whether the JavaScript execution has already been sent to the
     * browser. It is no longer possible to cancel the execution or add a
     * handler for the result after the execution has been sent.
     *
     * @return <code>true</code> if the execution is sent to the browser,
     *         <code>false</code> if it's still pending
     */
    boolean isSentToBrowser();

    /**
     * Adds a typed handler that will be run for a successful exception and a
     * handler that will be run for a failed execution. One of the handlers will
     * be invoked asynchronously when the result of the execution is sent back
     * to the server.
     * <p>
     * Handlers can only be added before the execution has been sent to the
     * browser.
     *
     * @param targetType
     *            the type to convert the JavaScript return value to, not
     *            <code>null</code>
     * @param resultHandler
     *            a handler for the return value from a successful execution,
     *            not <code>null</code>
     * @param errorHandler
     *            a handler for an error message in case the execution failed,
     *            or <code>null</code> to ignore errors
     */
    default <T> void then(Class<T> targetType,
            SerializableConsumer<T> resultHandler,
            SerializableConsumer<String> errorHandler) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }
        if (resultHandler == null) {
            throw new IllegalArgumentException("Result handler cannot be null");
        }

        SerializableConsumer<JsonValue> convertingResultHandler = value -> resultHandler
                .accept(JsonCodec.decodeAs(value, targetType));

        then(convertingResultHandler, errorHandler);
    }

    /**
     * Adds a typed handler that will be run for a successful exception. The
     * handler will be invoked asynchronously if the execution was successful.
     * In case of a failure, no handler will be run.
     * <p>
     * A handler can only be added before the execution has been sent to the
     * browser.
     *
     * @param targetType
     *            the type to convert the JavaScript return value to, not
     *            <code>null</code>
     * @param resultHandler
     *            a handler for the return value from a successful execution,
     *            not <code>null</code>
     */
    default <T> void then(Class<T> targetType,
            SerializableConsumer<T> resultHandler) {
        then(targetType, resultHandler, null);
    }

    /**
     * Creates a typed completable future that will be completed with the result
     * of the execution. It will be completed asynchronously when the result of
     * the execution is sent back to the server.
     * <p>
     * A completable future can only be created before the execution has been
     * sent to the browser.
     *
     * @param targetType
     *            the type to convert the JavaScript return value to, not
     *            <code>null</code>
     *
     * @return a completable future that will be completed based on the
     *         execution results, not <code>null</code>
     */
    default <T> CompletableFuture<T> toCompletableFuture(Class<T> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }

        CompletableFuture<T> completableFuture = new CompletableFuture<>();

        then(value -> {
            T convertedValue = JsonCodec.decodeAs(value, targetType);
            completableFuture.complete(convertedValue);
        }, errorValue -> {
            JavaScriptException exception = new JavaScriptException(errorValue);
            completableFuture.completeExceptionally(exception);
        });

        return completableFuture;
    }

    /**
     * Adds an untyped handler that will be run for a successful exception and a
     * handler that will be run for a failed execution. One of the handlers will
     * be invoked asynchronously when the result of the execution is sent back
     * to the server.
     * <p>
     * Handlers can only be added before the execution has been sent to the
     * browser.
     *
     * @param resultHandler
     *            a handler for the JSON representation of the value from a
     *            successful execution, not <code>null</code>
     * @param errorHandler
     *            a handler for an error message in case the execution failed,
     *            or <code>null</code> to ignore errors
     */
    void then(SerializableConsumer<JsonValue> resultHandler,
            SerializableConsumer<String> errorHandler);

    /**
     * Adds an untyped handler that will be run for a successful exception. The
     * handler will be invoked asynchronously if the execution was successful.
     * In case of a failure, no handler will be run.
     * <p>
     * A handler can only be added before the execution has been sent to the
     * browser.
     *
     * @param resultHandler
     *            a handler for the JSON representation of the return value from
     *            a successful execution, not <code>null</code>
     */
    default void then(SerializableConsumer<JsonValue> resultHandler) {
        then(resultHandler, null);
    }

    /**
     * Creates an untyped completable future that will be completed with the
     * result of the execution. It will be completed asynchronously when the
     * result of the execution is sent back to the server.
     * <p>
     * A completable future can only be created before the execution has been
     * sent to the browser.
     *
     * @return a completable future that will be completed based on the
     *         execution results, not <code>null</code>
     */
    default CompletableFuture<JsonValue> toCompletableFuture() {
        return toCompletableFuture(JsonValue.class);
    }
}
