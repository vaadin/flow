/*
 * Copyright 2000-2014 Vaadin Ltd.
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

import java.util.Collection;
import java.util.LinkedHashMap;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.Console;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * Manages the queue of server invocations (RPC) which are waiting to be sent to
 * the server.
 *
 * @since 7.6
 * @author Vaadin Ltd
 */
public class ServerRpcQueue {

    /**
     * The pending method invocations that will be send to the server by
     * {@link #sendPendingCommand}. The key is defined differently based on
     * whether the method invocation is enqueued with lastonly. With lastonly
     * enabled, the method signature ( {@link MethodInvocation#getLastOnlyTag()}
     * ) is used as the key to make enable removing a previously enqueued
     * invocation. Without lastonly, an incremental id based on
     * {@link #lastInvocationTag} is used to get unique values.
     */
    private LinkedHashMap<String, MethodInvocation> pendingInvocations = new LinkedHashMap<String, MethodInvocation>();

    private int lastInvocationTag = 0;

    protected ApplicationConnection connection;
    private boolean flushPending = false;

    private boolean flushScheduled = false;

    public ServerRpcQueue() {

    }

    /**
     * Sets the application connection this instance is connected to. Called
     * internally by the framework.
     *
     * @param connection
     *            the application connection this instance is connected to
     */
    public void setConnection(ApplicationConnection connection) {
        this.connection = connection;
    }

    /**
     * Adds an explicit RPC method invocation to the send queue.
     *
     * @param invocation
     *            RPC method invocation
     * @param delayed
     *            <code>false</code> to trigger sending within a short time
     *            window (possibly combining subsequent calls to a single
     *            request), <code>true</code> to let the framework delay sending
     *            of RPC calls and variable changes until the next non-delayed
     *            change
     * @param lastOnly
     *            <code>true</code> to remove all previously delayed invocations
     *            of the same method that were also enqueued with lastonly set
     *            to <code>true</code>. <code>false</code> to add invocation to
     *            the end of the queue without touching previously enqueued
     *            invocations.
     */
    public void add(MethodInvocation invocation, boolean lastOnly) {
        if (!connection.isApplicationRunning()) {
            Console.warn(
                    "Trying to invoke method on not yet started or stopped application");
            return;
        }
        String tag;
        if (lastOnly) {
            tag = invocation.getLastOnlyTag();
            assert !tag.matches(
                    "\\d+") : "getLastOnlyTag value must have at least one non-digit character";
            pendingInvocations.remove(tag);
        } else {
            tag = Integer.toString(lastInvocationTag++);
        }
        pendingInvocations.put(tag, invocation);
    }

    /**
     * Returns a collection of all queued method invocations
     * <p>
     * The returned collection must not be modified in any way
     *
     * @return a collection of all queued method invocations
     */
    public Collection<MethodInvocation> getAll() {
        return pendingInvocations.values();
    }

    /**
     * Clears the queue
     */
    public void clear() {
        pendingInvocations.clear();
        // Keep tag string short
        lastInvocationTag = 0;
        flushPending = false;
    }

    /**
     * Returns the current size of the queue
     *
     * @return the number of invocations in the queue
     */
    public int size() {
        return pendingInvocations.size();
    }

    /**
     * Returns the server RPC queue for the given application
     *
     * @param connection
     *            the application connection which owns the queue
     * @return the server rpc queue for the given application
     */
    public static ServerRpcQueue get(ApplicationConnection connection) {
        return connection.getServerRpcQueue();
    }

    /**
     * Checks if the queue is empty
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
        if (flushScheduled) {
            return;
        }
        flushPending = true;
        flushScheduled = true;
        Scheduler.get().scheduleFinally(scheduledFlushCommand);
    }

    private final ScheduledCommand scheduledFlushCommand = new ScheduledCommand() {
        @Override
        public void execute() {
            flushScheduled = false;
            if (!isFlushPending()) {
                // Somebody else cleared the queue before we had the chance
                return;
            }
            connection.getMessageSender().sendInvocationsToServer();
        }
    };

    /**
     * Checks if a flush operation is pending
     *
     * @return true if a flush is pending, false otherwise
     */
    public boolean isFlushPending() {
        return flushPending;
    }

    /**
     * Checks if a loading indicator should be shown when the RPCs have been
     * sent to the server and we are waiting for a response
     *
     * @return true if a loading indicator should be shown, false otherwise
     */
    public boolean showLoadingIndicator() {
        return true;
    }

    /**
     * Returns the current invocations as JSON
     *
     * @return the current invocations in a JSON format ready to be sent to the
     *         server
     */
    public JsonArray toJson() {
        JsonArray json = Json.createArray();
        if (isEmpty()) {
            return json;
        }

        for (MethodInvocation invocation : getAll()) {
            String connectorId = invocation.getConnectorId();
            JsonArray invocationJson = Json.createArray();
            invocationJson.set(0, connectorId);
            invocationJson.set(1, invocation.getInterfaceName());
            invocationJson.set(2, invocation.getMethodName());
            invocationJson.set(3, invocation.getParameters());
        }

        return json;
    }

}
