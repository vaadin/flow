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

import java.time.Duration;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * once a client-to-server RPC invocation has been handled, whether it completed
 * normally or threw. It is fired on the same thread as the matching
 * {@link RpcInvocationStartedEvent}.
 * <p>
 * The event tells how long the invocation took and whether it threw, so a
 * listener that times invocations needs no state of its own and no listener for
 * the started or failed event.
 *
 * @see AbstractRpcInvocationEvent
 * @since 25.3
 */
public class RpcInvocationEndedEvent extends AbstractRpcInvocationEvent {

    private final Duration duration;
    private final transient Throwable error;

    /**
     * Creates a new event for an invocation that completed normally and whose
     * duration was not measured.
     * <p>
     * The event reports a {@link Duration#ZERO zero} duration and no error. Use
     * {@link #RpcInvocationEndedEvent(UI, String, int, String, Duration, Throwable)}
     * to create an event with a measured duration or an error.
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
    public RpcInvocationEndedEvent(UI ui, String type, int nodeId,
            String name) {
        this(ui, type, nodeId, name, Duration.ZERO, null);
    }

    /**
     * Creates a new event carrying the measured duration of the invocation and
     * the throwable it raised, if any.
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
     * @param duration
     *            the time from just before the started event was fired until
     *            the invocation was handled, not {@code null}
     * @param error
     *            the throwable raised by the invocation handler, or
     *            {@code null} if it completed normally
     */
    public RpcInvocationEndedEvent(UI ui, String type, int nodeId, String name,
            Duration duration, Throwable error) {
        super(ui, type, nodeId, name);
        this.duration = duration;
        this.error = error;
    }

    /**
     * Gets how long the invocation took, measured from just before the
     * {@link RpcInvocationStartedEvent} was fired until the invocation was
     * handled. It includes the time the listeners of the started and failed
     * events took, but not the time spent on this event.
     *
     * @return the duration, never negative
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Gets the throwable raised by the invocation handler, which is the one the
     * matching {@link RpcInvocationFailedEvent} carried. An empty optional
     * means the invocation completed normally.
     *
     * @return the throwable, or an empty optional if the invocation did not
     *         throw
     */
    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }
}
