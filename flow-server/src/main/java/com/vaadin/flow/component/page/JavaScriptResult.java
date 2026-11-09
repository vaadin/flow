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
 * The result of a JavaScript execution, which hands the value that the
 * JavaScript returns to the server as a value of the type {@code T}.
 * <p>
 * A method of a {@link com.vaadin.flow.js.JsDefinition JavaScript definition}
 * declares this as its return type, with the type to convert the value to as
 * the type argument:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface DeepThoughtJs {
 *     &#64;JsExpression("return 42")
 *     JavaScriptResult&lt;Integer&gt; getAnswer();
 * }
 *
 * element.executeJs(DeepThoughtJs.class).getAnswer()
 *         .then(answer -&gt; Notification.show("The answer is " + answer));
 * </pre>
 *
 * The value is converted with Jackson, so every type that Jackson can
 * deserialize works, including beans and generic types such as
 * {@code List<Person>}.
 * <p>
 * A JavaScript <code>null</code> is handed over as a Java <code>null</code>,
 * also when {@code T} is a JSON node type such as {@code ObjectNode}. The one
 * exception is the <code>then</code> handlers of
 * {@code JavaScriptResult<JsonNode>}, which get the JSON as the browser sent
 * it, the same way as with {@link PendingJavaScriptResult}: there, a JavaScript
 * <code>null</code> is a {@code NullNode}. Its
 * <code>toCompletableFuture()</code> still completes with a Java
 * <code>null</code>.
 * <p>
 * If any of the <code>then</code> or <code>toCompletableFuture</code> methods
 * have been invoked before the JavaScript is sent to the browser, then there
 * will be an additional round trip for sending the result back to any
 * registered handler. If the JavaScript returns a <code>Promise</code>, then
 * the result will be sent to the server only when it is resolved.
 * <p>
 * It is not possible to add handlers after the execution has been sent to the
 * browser since the original result would in that case already be discarded.
 * <p>
 * {@link PendingJavaScriptResult} is the variant where the value is the JSON
 * that the browser sent back, and that can be converted to a type chosen when a
 * handler is added.
 *
 * @param <T>
 *            the type of the value of a successful execution
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
     * that will be run for a failed execution. One of the handlers will be
     * invoked asynchronously when the result of the execution is sent back to
     * the server. It is not possible to synchronously wait for the result of
     * the execution while holding the session lock since the request handling
     * thread that makes the result available will also need to lock the
     * session.
     * <p>
     * Handlers can only be added before the execution has been sent to the
     * browser.
     *
     * @param resultHandler
     *            a handler for the value from a successful execution, not
     *            <code>null</code>
     * @param errorHandler
     *            a handler for an error message in case the execution failed or
     *            its value could not be converted to {@code T}, or
     *            <code>null</code> to ignore failed executions. Without an
     *            error handler, a value that can not be converted is logged as
     *            a warning
     */
    void then(SerializableConsumer<T> resultHandler,
            SerializableConsumer<String> errorHandler);

    /**
     * Adds a handler that will be run for a successful execution. The handler
     * will be invoked asynchronously if the execution was successful. In case
     * the execution fails, the error is silently ignored: no handler is run and
     * nothing is logged or thrown. Use
     * {@link #then(SerializableConsumer, SerializableConsumer)} to find out
     * about a failed execution. A value that can not be converted to {@code T}
     * is logged as a warning.
     * <p>
     * A handler can only be added before the execution has been sent to the
     * browser.
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
     * the execution. It will be completed asynchronously when the result of the
     * execution is sent back to the server. It is not possible to synchronously
     * wait for the result of the execution while holding the session lock since
     * the request handling thread that makes the result available will also
     * need to lock the session.
     * <p>
     * If the execution fails, or its value can not be converted to {@code T},
     * the future is completed exceptionally with a {@link JavaScriptException}.
     * <p>
     * A completable future can only be created before the execution has been
     * sent to the browser.
     *
     * @return a completable future that will be completed based on the
     *         execution results, not <code>null</code>
     */
    default CompletableFuture<T> toCompletableFuture() {
        // The session that is locked when subscribing is the one that will
        // complete the future, since subscribing is only possible before the
        // execution is sent to the browser
        CompletableFuture<T> completableFuture = new DeadlockDetectingCompletableFuture<>(
                VaadinSession.getCurrent());
        then(completableFuture::complete, errorValue -> completableFuture
                .completeExceptionally(new JavaScriptException(errorValue)));
        return completableFuture;
    }
}
