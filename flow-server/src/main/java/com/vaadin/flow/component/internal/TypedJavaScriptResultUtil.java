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
package com.vaadin.flow.component.internal;

import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult.JavaScriptException;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.VaadinSession;

/**
 * Adds handlers to a pending JavaScript result that decode its JSON value to a
 * Java type. A value that can not be decoded is reported as an error instead of
 * being thrown from the handler, since the pending result chains the handlers
 * added to it, so throwing would also skip any added after this one, and a
 * future waiting for the value would never complete.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class TypedJavaScriptResultUtil {

    private TypedJavaScriptResultUtil() {
        // Only static helpers
    }

    /**
     * Adds a handler for the decoded value of a successful execution and a
     * handler for a failed execution or a value that can not be decoded.
     *
     * @param <T>
     *            the decoded type
     * @param pending
     *            the pending result to add the handlers to, not
     *            <code>null</code>
     * @param decoder
     *            the function that decodes the JSON value, throwing an
     *            {@link IllegalArgumentException} if it can not, not
     *            <code>null</code>
     * @param resultHandler
     *            a handler for the decoded value, not <code>null</code>
     * @param errorHandler
     *            a handler for an error message in case the execution failed or
     *            its value can not be decoded, or <code>null</code> to ignore
     *            errors
     */
    public static <T> void then(PendingJavaScriptResult pending,
            SerializableFunction<JsonNode, T> decoder,
            SerializableConsumer<T> resultHandler,
            SerializableConsumer<String> errorHandler) {
        SerializableConsumer<IllegalArgumentException> conversionErrorHandler;
        if (errorHandler != null) {
            conversionErrorHandler = e -> errorHandler.accept(e.getMessage());
        } else {
            conversionErrorHandler = e -> LoggerFactory
                    .getLogger(TypedJavaScriptResultUtil.class)
                    .warn("Ignored a JavaScript result that can not be"
                            + " converted", e);
        }
        pending.then(
                decodingHandler(decoder, resultHandler, conversionErrorHandler),
                errorHandler);
    }

    /**
     * Creates a completable future that is completed with the decoded value of
     * a successful execution. It is completed exceptionally with a
     * {@link JavaScriptException} if the execution fails, or if the value can
     * not be decoded, in which case the cause is the
     * {@link IllegalArgumentException} from decoding.
     *
     * @param <T>
     *            the decoded type
     * @param pending
     *            the pending result to add the handlers to, not
     *            <code>null</code>
     * @param decoder
     *            the function that decodes the JSON value, throwing an
     *            {@link IllegalArgumentException} if it can not, not
     *            <code>null</code>
     * @return a completable future that will be completed based on the
     *         execution results, not <code>null</code>
     */
    public static <T> CompletableFuture<T> toCompletableFuture(
            PendingJavaScriptResult pending,
            SerializableFunction<JsonNode, T> decoder) {
        /*
         * Assuming that subscription is only done for the currently locked
         * session. This is quite safe since you cannot subscribe any more after
         * the request has been sent to the client.
         *
         * This is the only way of catching the potentially common case of
         * blocking on a pending result for an element that hasn't yet even been
         * attached to a session.
         */
        CompletableFuture<T> completableFuture = new DeadlockDetectingCompletableFuture<>(
                VaadinSession.getCurrent());
        pending.then(decodingHandler(decoder, completableFuture::complete,
                e -> completableFuture.completeExceptionally(
                        new JavaScriptException(e.getMessage()).initCause(e))),
                errorValue -> completableFuture.completeExceptionally(
                        new JavaScriptException(errorValue)));
        return completableFuture;
    }

    private static <T> SerializableConsumer<JsonNode> decodingHandler(
            SerializableFunction<JsonNode, T> decoder,
            SerializableConsumer<T> resultHandler,
            SerializableConsumer<IllegalArgumentException> conversionErrorHandler) {
        return value -> {
            T decodedValue;
            try {
                decodedValue = decoder.apply(value);
            } catch (IllegalArgumentException e) {
                conversionErrorHandler.accept(e);
                return;
            }
            resultHandler.accept(decodedValue);
        };
    }
}
