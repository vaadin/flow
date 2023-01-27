/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.server.communication.rpc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract invocation handler implementation with common methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class AbstractRpcInvocationHandler
        implements RpcInvocationHandler {

    @Override
    public Optional<Runnable> handle(UI ui, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_NODE);
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));
        if (node == null) {
            getLogger().debug("Ignoring RPC for non-existent node: {}",
                    getNodeId(invocationJson));
            return Optional.empty();
        }
        if (!node.isAttached()) {
            getLogger().debug("Ignoring RPC for detached node: {}",
                    getNodeId(invocationJson));
            return Optional.empty();
        }

        // ignore RPC requests from the client side for the nodes that are
        // invisible, disabled or inert
        if (node.isInactive()) {
            getLogger().trace("Ignored RPC for invocation handler '{}' from "
                    + "the client side for an inactive (disabled or invisible) node id='{}'",
                    getClass().getName(), node.getId());
            return Optional.empty();
        } else if (!allowInert(ui, invocationJson) && node.isInert()) {
            getLogger().trace(
                    "Ignored RPC for invocation handler '{}' from "
                            + "the client side for an inert node id='{}'",
                    getClass().getName(), node.getId());
            return Optional.empty();
        } else {
            return handleNode(node, invocationJson);
        }
    }

    /**
     * Checks whether a Poll RPC invocation is valid or not.
     *
     * @param ui
     *            the current UI instance
     * @param invocationJson
     *            the JsonObject containing invocation properties
     * @return a boolean indicating that the Poll RPC invocation is valid or
     *         not.
     */
    private boolean isValidPollInvocation(UI ui, JsonObject invocationJson) {

        if (!isPollEventInvocation(invocationJson)) {
            return false;
        }

        if (!isPollingEnabledForUI(ui)) {
            getLogger().warn(
                    "Ignoring Poll RPC for UI that does not have polling enabled.");
            getLogger().debug("Ignored payload:\n{}", invocationJson);
            return false;
        }

        if (!isLegitimatePollEventInvocation(ui, invocationJson)) {
            getLogger().warn(
                    "Ignoring Poll RPC for illegitimate invocation payload.");
            getLogger().debug("Ignored payload:\n{}", invocationJson);
            return false;
        }

        return true;
    }

    private boolean isPollEventInvocation(JsonObject invocationJson) {
        return invocationJson.hasKey(JsonConstants.RPC_EVENT_TYPE)
                && PollEvent.DOM_EVENT_NAME.equalsIgnoreCase(
                        invocationJson.getString(JsonConstants.RPC_EVENT_TYPE));
    }

    private boolean isPollingEnabledForUI(UI ui) {
        return ui.getPollInterval() > 0;
    }

    /**
     * This method checks that a legitimate Poll Rpc invocation properties
     * should contain only the following three <b>allowed</b> keys along with
     * their values and nothing less or more:
     * <ul>
     * <li>{@link com.vaadin.flow.shared.JsonConstants#RPC_TYPE}</li>
     * <li>{@link com.vaadin.flow.shared.JsonConstants#RPC_NODE}</li>
     * <li>{@link com.vaadin.flow.shared.JsonConstants#RPC_EVENT_TYPE}</li>
     * </ul>
     * <p>
     * As Rpc invocations of type polling would still be handled even while the
     * UI is inert (due to server-modality) this will make sure that the request
     * does not include any extra malicious payloads.
     * <p>
     * This method checks the existence of first two allowed keys as the
     * {@link #isPollEventInvocation(JsonObject)} had already checked for the
     * existence of the
     * {@link com.vaadin.flow.shared.JsonConstants#RPC_EVENT_TYPE} before this
     * method is called.
     *
     * @see #isValidPollInvocation(UI, JsonObject)
     *
     * @param ui
     *            the UI instance which the Rpc event is coming from.
     * @param invocationJson
     *            the Rpc invocation payload as Json.
     * @return a boolean indicating whether the invocationJson is legitimate in
     *         accordance with the UI instance.
     */
    private boolean isLegitimatePollEventInvocation(UI ui,
            JsonObject invocationJson) {
        List<String> allowedKeys = Arrays.asList(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_NODE, JsonConstants.RPC_EVENT_TYPE);
        List<String> invocationKeys = Arrays.asList(invocationJson.keys());
        if (!allowedKeys.containsAll(invocationKeys)) {
            return false;
        }

        if (!invocationJson.hasKey(JsonConstants.RPC_TYPE)) {
            return false;
        }
        if (!JsonConstants.RPC_TYPE_EVENT
                .equals(invocationJson.getString(JsonConstants.RPC_TYPE))) {
            return false;
        }

        // Polling events should target only the root component in a UI:
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));
        return node.equals(ui.getElement().getNode());
    }

    /**
     * Specifies whether inert status should be ignored for an RPC invocation or
     * not. The default behaviour is to let the polling events be handled, while
     * ignoring other requests.
     *
     * @param ui
     *            the UI instance that RPC invocation originated from.
     * @param invocationJson
     *            the JsonObject containing invocation properties.
     * @return a boolean indicating that the inert status should be ignored for
     *         the current invocation or not.
     */
    protected boolean allowInert(UI ui, JsonObject invocationJson) {
        return isValidPollInvocation(ui, invocationJson);
    }

    /**
     * Handle the RPC data {@code invocationJson} using target {@code node} as a
     * context.
     *
     * @param node
     *            node to handle invocation with, not {@code null}
     * @param invocationJson
     *            the RPC data to handle, not {@code null}
     * @return an optional runnable
     */
    protected abstract Optional<Runnable> handleNode(StateNode node,
            JsonObject invocationJson);

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(AbstractRpcInvocationHandler.class.getName());
    }

    protected static int getNodeId(JsonObject invocationJson) {
        return (int) invocationJson.getNumber(JsonConstants.RPC_NODE);
    }
}
