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

    private boolean sentToBrowser = false;
    private boolean canceled = false;

    /**
     * The UI internals whose undelivered invocation count includes this
     * invocation, or <code>null</code> if it is not counted, either because its
     * owner is not attached to a UI or because it is no longer waiting to be
     * sent.
     * <p>
     * Only ever set to the internals of the UI that the owner belongs to, so
     * this reaches nothing that the owner does not reach anyway. That also
     * makes it safe to serialize: an invocation restored with its UI keeps
     * updating the count that was restored with it.
     */
    private UIInternals countedIn;

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
     * expression.
     *
     * @return <code>true</code> if the return value should be passed back from
     *         the client, <code>false</code> if the return value can be ignored
     */
    public boolean isSubscribed() {
        return successHandler != null;
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

        successHandler.accept(value);
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
    }

    @Override
    public boolean cancelExecution() {
        if (sentToBrowser || canceled) {
            return false;
        }
        canceled = true;
        stopCounting();

        if (errorHandler != null) {
            errorHandler.accept(EXECUTION_CANCELED);
        }

        return true;
    }

    @Override
    public boolean isSentToBrowser() {
        return sentToBrowser;
    }

    /**
     * Counts this invocation as undelivered in the UI that its owner belongs
     * to, unless it is already counted or no longer waiting to be sent.
     * <p>
     * An invocation is not counted when it is created, since its owner is not
     * necessarily attached to a UI then, and an owner that is detached, or
     * attached to a UI that has been closed since, has no count to be part of.
     * It starts counting once the owner is attached, which is also when it
     * starts being on its way to a client. Called by the framework when the
     * owner is attached, and when the invocation is queued for a UI.
     */
    public void countWhenAttached() {
        if (countedIn != null || sentToBrowser || canceled) {
            return;
        }
        countedIn = PendingJavaScriptInvocationUtil.invocationScheduled(this);
    }

    // Non-private for testing purposes
    void setSentToBrowser() {
        assert !sentToBrowser;
        sentToBrowser = true;
        stopCounting();
    }

    /**
     * Stops counting this invocation as undelivered in the UI it was counted
     * in, without otherwise changing its state.
     * <p>
     * Called when the invocation stops waiting to be sent, either because it
     * has been sent or canceled, or because the framework discards it, for
     * instance when its owner is detached. It is counted again through
     * {@link #countWhenAttached()} if it ends up waiting to be sent again.
     */
    public void stopCounting() {
        if (countedIn == null) {
            return;
        }
        if (countedIn.getSession() != null) {
            countedIn.addUndeliveredJsInvocations(-1);
        }
        // A closed UI has no session to lock and its count is discarded with
        // it, so there is nothing to update for one
        countedIn = null;
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
