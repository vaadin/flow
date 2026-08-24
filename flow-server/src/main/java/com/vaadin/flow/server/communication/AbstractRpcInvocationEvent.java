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

import java.util.EventObject;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Describes which client-to-server RPC invocation (a DOM event, a synchronized
 * property update, a {@code @ClientCallable}/template event handler, a
 * server-side navigation, a return channel message, and so on) an event fired
 * through the {@link VaadinService#getEventBus() service event bus} is about.
 * <p>
 * A client request can carry several invocations, and one event of each type is
 * fired per invocation. Listeners are added for the concrete event types
 * {@link RpcInvocationStartedEvent}, {@link RpcInvocationFailedEvent} and
 * {@link RpcInvocationEndedEvent}, since the event bus dispatches events by
 * their exact type.
 * <p>
 * Being reported does not mean the invocation had an effect: an RPC targeting a
 * node that is detached, disabled or inert is reported and only then discarded
 * unhandled by the handler it is routed to. A property synchronization is the
 * exception, since its events are tied to the change event described below and
 * are absent entirely when the update is discarded.
 * <p>
 * The started, (optional) failed and ended events of one invocation are fired
 * on the same thread, in that order, with the ended event always fired after
 * the started one regardless of outcome, so a listener may keep timing state in
 * a {@link ThreadLocal}. Within the handling of one request the events do not
 * nest: those of one invocation are all fired before those of the next.
 * Requests belonging to different sessions are handled concurrently, however,
 * so a listener on the service event bus must expect invocations of several
 * sessions to be in flight on several threads at once.
 * <p>
 * Synchronized property updates ({@code mSync}) deserve a few remarks, because
 * they are handled in two steps: the value of every synchronized property in
 * the request is applied to the state tree first, and only then are the
 * corresponding property change events fired, so that application code sees a
 * fully updated tree.
 * <ul>
 * <li>The events surround the second step, the property change event, since
 * that is the step that runs application code. When the update produces no
 * change event they therefore surround no work: this is the case when the value
 * was already the one the client sent, when a model filter rejects the update,
 * and when the property is bound to a signal through
 * {@code Element.bindProperty}, whose write callback runs in the first step
 * instead.</li>
 * <li>The first step can fail on its own, when the property is not synchronized
 * at all or a signal bound to it rejects the write. There is then no change
 * event to surround, so the events are fired at that point instead, with a
 * {@link RpcInvocationFailedEvent} carrying the failure. Refusing a value the
 * client should not have sent aborts the request, so unlike a failure of the
 * application code an invocation runs, the remaining invocations of the request
 * are not handled and the client is sent an internal error.</li>
 * <li>Because the first step is completed for the whole request up front, all
 * property updates in a request are reported before any other invocation it
 * carries, even those the client sent earlier in the request.</li>
 * </ul>
 *
 * @since 25.3
 */
public abstract class AbstractRpcInvocationEvent extends EventObject {

    private final String type;
    private final int nodeId;
    private final String name;

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI the invocation is handled against, not {@code null}
     * @param type
     *            the protocol-level invocation type (for example {@code event},
     *            {@code mSync}, {@code publishedEventHandler},
     *            {@code navigation}, {@code channel}), not {@code null}
     * @param nodeId
     *            the id of the targeted {@code StateNode}, or {@code -1} if the
     *            invocation does not target a node
     * @param name
     *            a human-readable identifier for the invocation (the DOM event
     *            name, the synchronized property name, the invoked method name,
     *            the navigation location, ...), or {@code null} if none applies
     */
    protected AbstractRpcInvocationEvent(UI ui, String type, int nodeId,
            String name) {
        super(ui);
        this.type = type;
        this.nodeId = nodeId;
        this.name = name;
    }

    /**
     * Gets the UI the invocation is handled against.
     *
     * @return the UI, not {@code null}
     */
    public UI getUI() {
        return (UI) getSource();
    }

    /**
     * Gets the protocol-level invocation type, for example {@code event},
     * {@code mSync}, {@code publishedEventHandler}, {@code navigation} or
     * {@code channel}.
     *
     * @return the invocation type, not {@code null}
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the id of the {@code StateNode} the invocation targets.
     *
     * @return the node id, or {@code -1} if the invocation does not target a
     *         node
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets a human-readable identifier for the invocation, such as the DOM
     * event name, the name of the synchronized property for {@code mSync}, the
     * invoked {@code @ClientCallable}/template method name, or the navigation
     * location.
     * <p>
     * The name never carries the data of the invocation, only its identity: for
     * a property synchronization it is the property name, not the value sent
     * from the client.
     *
     * @return the invocation name, or {@code null} if none applies
     */
    public String getName() {
        return name;
    }
}
