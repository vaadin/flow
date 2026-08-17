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

import java.io.Serializable;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Listener that is notified around the server-side handling of individual
 * client-to-server RPC invocations, enabling per-invocation observation (for
 * example to emit a tracing span showing which DOM event, synchronized property
 * or {@code @ClientCallable} method is consuming the time spent holding the
 * session lock while processing a request).
 * <p>
 * A single client request typically carries several invocations; the listener
 * is notified once per invocation. Being notified does not mean the invocation
 * had an effect: an RPC targeting a node that is detached, disabled or inert is
 * reported and only then discarded unhandled by the handler it is routed to. A
 * property synchronization is the exception, since its notifications are tied
 * to the change event described below and are absent entirely when the update
 * is discarded.
 * <p>
 * {@link #invocationStarted}, {@link #invocationFailed} (only when the handler
 * threw) and {@link #invocationEnded} for one invocation are delivered on the
 * same thread, in that order, with {@code invocationEnded} always delivered
 * after {@code invocationStarted} regardless of outcome, so a listener may keep
 * timing state in a {@link ThreadLocal}. Within the handling of one request the
 * notifications do not nest: the callbacks of one invocation complete before
 * those of the next begin. Requests belonging to different sessions are handled
 * concurrently, however, so a listener registered on the service must expect
 * invocations of several sessions to be in flight on several threads at once.
 * <p>
 * Synchronized property updates ({@code mSync}) deserve two remarks, because
 * they are handled in two steps: the value of every synchronized property in
 * the request is applied to the state tree first, and only then are the
 * corresponding property change events fired, so that application code sees a
 * fully updated tree.
 * <ul>
 * <li>The notifications surround the second step, the property change event,
 * since that is the step that runs application code. When the update produces
 * no change event the callbacks therefore surround no work: this is the case
 * when the value was already the one the client sent, when a model filter
 * rejects the update, and when the property is bound to a signal through
 * {@code Element.bindProperty}, whose listeners run while the value is being
 * written to the signal in the first step. Failures of a signal-bound update
 * consequently reach the session error handler without being reported to
 * {@link #invocationFailed}.</li>
 * <li>Because the first step is completed for the whole request up front, all
 * property updates in a request are reported before any other invocation it
 * carries, even those the client sent earlier in the request.</li>
 * </ul>
 * <p>
 * Implementations must be fast and non-blocking: callbacks run on the request
 * thread directly around invocation handling. Exceptions thrown from a callback
 * are logged and suppressed so they cannot disrupt RPC processing.
 * <p>
 * Register via
 * {@link VaadinService#addRpcInvocationListener(RpcInvocationListener)},
 * typically from a {@link VaadinServiceInitListener}.
 *
 * @see VaadinService#addRpcInvocationListener(RpcInvocationListener)
 * @see RpcInvocationEvent
 * @since 25.2
 */
public interface RpcInvocationListener extends Serializable {

    /**
     * Invoked on the request thread immediately before an RPC invocation is
     * handled.
     *
     * @param event
     *            the invocation event
     */
    default void invocationStarted(RpcInvocationEvent event) {
    }

    /**
     * Invoked on the request thread when handling an invocation threw, before
     * {@link #invocationEnded}. The framework routes the throwable to the
     * session error handler independently of this callback.
     *
     * @param event
     *            the invocation event
     * @param error
     *            the throwable raised by the invocation handler
     */
    default void invocationFailed(RpcInvocationEvent event, Throwable error) {
    }

    /**
     * Invoked on the request thread once an RPC invocation has been handled,
     * whether it completed normally or threw.
     *
     * @param event
     *            the invocation event
     */
    default void invocationEnded(RpcInvocationEvent event) {
    }
}
