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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.component.internal.DeadlockDetectingCompletableFuture;
import com.vaadin.flow.component.page.PendingJavaScriptResult.JavaScriptException;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinSession;

/**
 * The result of a JavaScript execution, with the value already converted to the
 * type {@code T}. {@link PendingJavaScriptResult} is the untyped variant, where
 * the value is the JSON that the browser sent back.
 * <p>
 * PROTOTYPE: name and shape are not decided.
 *
 * @param <T>
 *            the type of the value of a successful execution
 * @since 25.4
 */
public interface JavaScriptResult<T> extends Serializable {

    /**
     * Cancel the javascript execution, if it was not yet sent to the browser
     * for execution.
     *
     * @return <code>true</code> if the execution was canceled,
     *         <code>false</code> if not
     */
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
     * Adds a handler that will be run for a successful execution and a handler
     * that will be run for a failed execution.
     *
     * @param resultHandler
     *            a handler for the value from a successful execution, not
     *            <code>null</code>
     * @param errorHandler
     *            a handler for an error message in case the execution failed,
     *            or <code>null</code> to ignore errors
     */
    void then(SerializableConsumer<T> resultHandler,
            SerializableConsumer<String> errorHandler);

    /**
     * Adds a handler that will be run for a successful execution. In case of a
     * failure, no handler will be run.
     *
     * @param resultHandler
     *            a handler for the value from a successful execution, not
     *            <code>null</code>
     */
    default void then(SerializableConsumer<T> resultHandler) {
        then(resultHandler, null);
    }

    /**
     * Creates a completable future that will be completed with the result of
     * the execution.
     *
     * @return a completable future that will be completed based on the
     *         execution results, not <code>null</code>
     */
    default CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> completableFuture = new DeadlockDetectingCompletableFuture<>(
                VaadinSession.getCurrent());
        then(completableFuture::complete, errorValue -> completableFuture
                .completeExceptionally(new JavaScriptException(errorValue)));
        return completableFuture;
    }
}
