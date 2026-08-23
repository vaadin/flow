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

/**
 * Event fired to {@link RpcInvocationListener}s around the server-side handling
 * of a single client-to-server RPC invocation (a DOM event, a
 * {@code @ClientCallable}/template event handler, a server-side navigation, a
 * return channel message, and so on).
 * <p>
 * A client request can carry several invocations; one event is fired per
 * invocation. The {@link RpcInvocationListener#invocationStarted}, (optional)
 * {@link RpcInvocationListener#invocationFailed} and
 * {@link RpcInvocationListener#invocationEnded} callbacks for a single
 * invocation are delivered on the same thread, so timing state can be kept in a
 * thread local.
 *
 * @see RpcInvocationListener
 * @since 25.2
 * @deprecated use {@link RpcInvocationStartedEvent},
 *             {@link RpcInvocationFailedEvent} and
 *             {@link RpcInvocationEndedEvent} on the
 *             {@link com.vaadin.flow.server.VaadinService#getEventBus() service
 *             event bus} instead
 */
@Deprecated(since = "25.3", forRemoval = true)
public class RpcInvocationEvent extends AbstractRpcInvocationEvent {

    /**
     * Creates a new RPC invocation event.
     *
     * @param ui
     *            the UI the invocation is handled against, not {@code null}
     * @param type
     *            the protocol-level invocation type (for example {@code event},
     *            {@code publishedEventHandler}, {@code navigation},
     *            {@code channel}), not {@code null}
     * @param nodeId
     *            the id of the targeted {@code StateNode}, or {@code -1} if the
     *            invocation does not target a node
     * @param name
     *            a human-readable identifier for the invocation (the DOM event
     *            name, the invoked method name, the navigation location, ...),
     *            or {@code null} if none applies
     */
    public RpcInvocationEvent(UI ui, String type, int nodeId, String name) {
        super(ui, type, nodeId, name);
    }
}
