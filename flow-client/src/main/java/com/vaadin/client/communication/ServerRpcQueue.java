/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.communication;

import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.Console;
import com.vaadin.client.Registry;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Manages the queue of server invocations (RPC) which are waiting to be sent to
 * the server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServerRpcQueue {

    private static final Runnable NO_OP = () -> {
        // NOOP
    };

    private JsonArray pendingInvocations = Json.createArray();

    private boolean flushPending = false;

    private final Registry registry;

    private Runnable doFlushStrategy = NO_OP;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public ServerRpcQueue(Registry registry) {
        this.registry = registry;
    }

    /**
     * Adds an explicit RPC method invocation to the send queue.
     *
     * @param invocation
     *            RPC method invocation
     */
    public void add(JsonValue invocation) {
        if (!registry.getUILifecycle().isRunning()) {
            Console.warn(
                    "Trying to invoke method on not yet started or stopped application");
            return;
        }
        pendingInvocations.set(pendingInvocations.length(), invocation);
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        pendingInvocations = Json.createArray();
        flushPending = false;
        doFlushStrategy = NO_OP;
    }

    /**
     * Returns the current size of the queue.
     *
     * @return the number of invocations in the queue
     */
    public int size() {
        return pendingInvocations.length();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Triggers a send of server RPC and legacy variable changes to the server.
     */
    public void flush() {
        if (isFlushScheduled() || isEmpty()) {
            return;
        }
        flushPending = true;

        doFlushStrategy = this::doFlush;
        // Deferred so we can be sure that all event handlers have been invoked
        // before flushing the queue
        Scheduler.get().scheduleDeferred(() -> doFlushStrategy.run());
    }

    /**
     * Checks if a flush operation is pending.
     *
     * @return true if a flush is pending, false otherwise
     */
    public boolean isFlushPending() {
        return flushPending;
    }

    /**
     * Checks if a loading indicator should be shown when the RPCs have been
     * sent to the server and we are waiting for a response.
     *
     * @return true if a loading indicator should be shown, false otherwise
     */
    public boolean showLoadingIndicator() {
        return true;
    }

    /**
     * Returns the current invocations as JSON.
     *
     * @return the current invocations in a JSON format ready to be sent to the
     *         server
     */
    public JsonArray toJson() {
        return pendingInvocations;
    }

    private boolean isFlushScheduled() {
        return NO_OP != doFlushStrategy;
    }

    private void doFlush() {
        doFlushStrategy = NO_OP;
        if (!isFlushPending()) {
            // Somebody else cleared the queue before we had the chance
            return;
        }
        registry.getMessageSender().sendInvocationsToServer();
    }

}
