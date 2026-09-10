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
 * when handling a client-to-server RPC invocation threw, before the matching
 * {@link RpcInvocationEndedEvent}. The framework routes the throwable to the
 * session error handler independently of this event.
 *
 * @see AbstractRpcInvocationEvent
 * @since 25.3
 */
public class RpcInvocationFailedEvent extends AbstractRpcInvocationEvent {

    private final transient Throwable error;

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
     * @param error
     *            the throwable raised by the invocation handler
     */
    public RpcInvocationFailedEvent(UI ui, String type, int nodeId, String name,
            Throwable error) {
        super(ui, type, nodeId, name);
        this.error = error;
    }

    /**
     * Gets the throwable raised by the invocation handler.
     *
     * @return the throwable
     */
    public Throwable getError() {
        return error;
    }
}
