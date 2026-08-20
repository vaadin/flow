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
package com.vaadin.flow.server.communication;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * immediately before an RPC invocation is handled.
 *
 * @see RpcInvocationListener#invocationStarted(RpcInvocationEvent)
 * @since 25.3
 */
public class RpcInvocationStartedEvent extends RpcInvocationEvent {

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI the invocation is handled against, not {@code null}
     * @param type
     *            the protocol-level invocation type, not {@code null}
     * @param nodeId
     *            the id of the targeted {@code StateNode}, or {@code -1} if the
     *            invocation does not target a node
     * @param name
     *            a human-readable identifier for the invocation, or
     *            {@code null} if none applies
     */
    public RpcInvocationStartedEvent(UI ui, String type, int nodeId,
            String name) {
        super(ui, type, nodeId, name);
    }

    /**
     * Creates a new event with the same invocation details as the given event.
     *
     * @param event
     *            the event to copy the invocation details from, not
     *            {@code null}
     */
    public RpcInvocationStartedEvent(RpcInvocationEvent event) {
        super(event.getUI(), event.getType(), event.getNodeId(),
                event.getName());
    }
}
