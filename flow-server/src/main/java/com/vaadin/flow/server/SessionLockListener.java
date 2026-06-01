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
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Listener that is notified when a Vaadin session lock is acquired and
 * released, enabling observation of session-lock contention (for example to
 * publish lock wait and hold-time metrics).
 * <p>
 * All Vaadin server-side work for a single session is serialized behind one
 * lock, so the time a thread blocks acquiring it and the time it holds it are
 * key performance signals. Callbacks are delivered for the <em>outermost</em>
 * acquisition only (reentrant re-locks are not reported), and the three
 * callbacks for a single hold are delivered on the same thread in the order
 * {@link #lockRequested}, {@link #lockAcquired}, {@link #lockReleased}, so a
 * listener can record the start time in a {@link ThreadLocal} on
 * {@code lockRequested}, derive the wait time on {@code lockAcquired}, and the
 * hold time on {@code lockReleased}.
 * <p>
 * Implementations must be fast and non-blocking: callbacks run on the
 * request/access thread directly around the lock operation. Exceptions thrown
 * from a callback are logged and suppressed so they cannot disrupt session
 * locking.
 * <p>
 * Register via
 * {@link VaadinService#addSessionLockListener(SessionLockListener)}, typically
 * from a {@link VaadinServiceInitListener}. Listeners registered after a
 * session's lock has already been created are still honoured.
 *
 * @see VaadinService#addSessionLockListener(SessionLockListener)
 * @see SessionLockEvent
 */
public interface SessionLockListener extends Serializable {

    /**
     * Invoked on the locking thread immediately before it attempts to acquire
     * the session lock for an outermost (non-reentrant) acquisition. The thread
     * may block between this callback and {@link #lockAcquired}; the elapsed
     * time is the lock wait time.
     *
     * @param event
     *            the session lock event
     */
    default void lockRequested(SessionLockEvent event) {
    }

    /**
     * Invoked on the locking thread immediately after it has acquired the
     * session lock for an outermost acquisition.
     *
     * @param event
     *            the session lock event
     */
    default void lockAcquired(SessionLockEvent event) {
    }

    /**
     * Invoked on the locking thread immediately after the outermost lock hold
     * has been released (the lock's hold count reached zero). The time since
     * {@link #lockAcquired} is the lock hold time.
     *
     * @param event
     *            the session lock event
     */
    default void lockReleased(SessionLockEvent event) {
    }
}
