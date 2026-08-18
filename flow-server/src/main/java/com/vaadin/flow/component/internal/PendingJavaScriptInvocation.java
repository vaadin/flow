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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.StateNode;

/**
 * A pending JavaScript result that can be sent to the client.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class PendingJavaScriptInvocation implements PendingJavaScriptResult {
    private static final String EXECUTION_CANCELED = "Execution canceled";

    private final JavaScriptInvocation invocation;
    private final StateNode owner;

    private SerializableConsumer<JsonNode> successHandler;
    private SerializableConsumer<String> errorHandler;
    private SerializableRunnable completionHandler;

    private boolean sentToBrowser = false;
    private boolean canceled = false;

    /**
     * Creates a new pending invocation for the given owner node and invocation.
     *
     * @param owner
     *            the state node that this invocation is related to, not
     *            <code>null</code>
     * @param invocation
     *            the JavaScript invocation to send, not <code>null</code>
     */
    public PendingJavaScriptInvocation(StateNode owner,
            JavaScriptInvocation invocation) {
        assert owner != null;
        assert invocation != null;

        this.owner = owner;
        this.invocation = invocation;
    }

    /**
     * Gets the state node that this invocation belongs to.
     *
     * @return the owner state node
     */
    public StateNode getOwner() {
        return owner;
    }

    /**
     * Checks if there are any subscribers for the return value of this
     * expression, either the application through
     * {@link #then(SerializableConsumer, SerializableConsumer)} or the
     * framework through {@link #addCompletionHandler(SerializableRunnable)}.
     *
     * @return <code>true</code> if the return value should be passed back from
     *         the client, <code>false</code> if the return value can be ignored
     */
    public boolean isSubscribed() {
        return successHandler != null || completionHandler != null;
    }

    /**
     * Checks if the application has subscribed to the return value of this
     * expression, as opposed to only the framework tracking when the invocation
     * is done.
     *
     * @return <code>true</code> if the return value is passed to a handler
     *         registered by the application
     * @since 25.3
     */
    public boolean isSubscribedByApplication() {
        return successHandler != null;
    }

    /**
     * Adds a handler that is run when this invocation is done, regardless of
     * whether it succeeded, failed or was canceled.
     * <p>
     * This is meant for the framework's own bookkeeping. Unlike
     * {@link #then(SerializableConsumer, SerializableConsumer)}, it doesn't
     * count as the application subscribing to the return value, but the outcome
     * still has to be passed back from the client for the handler to be run, so
     * this invocation becomes {@link #isSubscribed() subscribed}.
     *
     * @param handler
     *            the handler to run when the invocation is done, not
     *            <code>null</code>
     * @since 25.3
     */
    public void addCompletionHandler(SerializableRunnable handler) {
        assert handler != null;

        if (canceled) {
            handler.run();
            return;
        }

        completionHandler = combineHandlers(completionHandler, handler);
    }

    /**
     * Completes this invocation with the given return value from the client.
     * Should only be used if there return value subscribers.
     *
     * @param value
     *            the JSON return value from the client
     * @since 25.0
     */
    public void complete(JsonNode value) {
        assert isSubscribed();

        if (successHandler != null) {
            successHandler.accept(value);
        }
        runCompletionHandler();
    }

    /**
     * Completes this invocation with the given exception value from the client.
     * Should only be used if there return value subscribers.
     *
     * @param value
     *            the JSON exception value from the client
     * @since 25.0
     */
    public void completeExceptionally(JsonNode value) {
        assert isSubscribed();

        String message = value.asString();

        if (errorHandler != null) {
            errorHandler.accept(message);
        } else {
            getLogger().debug("Ignored error from JavaScript: {}", message);
        }
        runCompletionHandler();
    }

    @Override
    public boolean cancelExecution() {
        if (sentToBrowser || canceled) {
            return false;
        }
        canceled = true;

        if (errorHandler != null) {
            errorHandler.accept(EXECUTION_CANCELED);
        }
        runCompletionHandler();

        return true;
    }

    @Override
    public boolean isSentToBrowser() {
        return sentToBrowser;
    }

    // Non-private for testing purposes
    void setSentToBrowser() {
        assert !sentToBrowser;
        sentToBrowser = true;
    }

    @Override
    public void then(SerializableConsumer<JsonNode> successHandler,
            SerializableConsumer<String> errorHandler) {
        if (successHandler == null) {
            throw new IllegalArgumentException(
                    "Success handler cannot be null");
        }

        if (sentToBrowser) {
            throw new IllegalStateException(
                    "Cannot subscribe to the return value after the execution has been sent to the client.");
        }

        if (canceled) {
            if (errorHandler != null) {
                errorHandler.accept(EXECUTION_CANCELED);
            }

            return;
        }

        this.successHandler = combineHandlers(this.successHandler,
                successHandler);
        this.errorHandler = combineHandlers(this.errorHandler, errorHandler);
    }

    private void runCompletionHandler() {
        if (completionHandler != null) {
            SerializableRunnable handler = completionHandler;
            completionHandler = null;
            handler.run();
        }
    }

    private static SerializableRunnable combineHandlers(
            SerializableRunnable first, SerializableRunnable second) {
        if (first == null) {
            return second;
        }
        return () -> {
            first.run();
            second.run();
        };
    }

    private static <T> SerializableConsumer<T> combineHandlers(
            SerializableConsumer<T> first, SerializableConsumer<T> second) {
        if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        } else {
            return value -> {
                first.accept(value);
                second.accept(value);
            };
        }
    }

    /**
     * Checks whether this invocation has been canceled.
     *
     * @return <code>true</code> if the invocation has been canceled, otherwise
     *         <code>false</code>
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Gets the underlying JavaScript invocation that this pending invocation is
     * based on.
     *
     * @return the JavaScript invocation, not <code>null</code>
     */
    public JavaScriptInvocation getInvocation() {
        return invocation;
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(PendingJavaScriptInvocation.class.getName());
    }

}
