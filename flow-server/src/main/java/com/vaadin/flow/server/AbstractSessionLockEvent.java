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

import java.util.EventObject;
import java.util.Optional;

import com.vaadin.flow.component.UI;

/**
 * Describes which service and session an event fired through the
 * {@link VaadinService#getEventBus() service event bus} around acquisition and
 * release of a Vaadin session lock is about.
 * <p>
 * The same lock instance protects a session whether it is acquired by the
 * framework while handling a request or via {@link VaadinSession#lock()} (for
 * example from {@link UI#access(Command)}). Events are only fired for the
 * outermost lock-hold: taking the lock again on a thread that already holds it
 * fires nothing. Flow measures the lock-hold itself:
 * {@link SessionLockAcquiredEvent#getWaitTime()} tells how long the thread
 * waited for the lock and {@link SessionLockReleasedEvent#getHoldTime()} how
 * long it held it.
 * <p>
 * All events of a lock-hold are fired on the thread that takes the lock. A
 * listener can therefore tell whether the lock is taken while handling a
 * request (an incoming push message included) or outside of one, for example
 * from {@link UI#access(Command)} in a background thread, by checking whether
 * {@link VaadinService#getCurrentRequest()} returns a request. Note that the
 * request may belong to another session, when a request thread locks a session
 * that is not its own.
 * <p>
 * Listeners are added for the concrete event types
 * {@link SessionLockRequestedEvent}, {@link SessionLockAcquiredEvent} and
 * {@link SessionLockReleasedEvent}, since the event bus dispatches events by
 * their exact type. The wait time and the hold time are only measured while
 * there is a listener for the event that reports them.
 * 
 * @since 25.3
 */
public abstract class AbstractSessionLockEvent extends EventObject {

    private final VaadinSession session;

    /**
     * Creates a new event without a session.
     *
     * @param service
     *            the Vaadin service whose session lock is being acquired or
     *            released, not {@code null}
     */
    protected AbstractSessionLockEvent(VaadinService service) {
        this(service, null);
    }

    /**
     * Creates a new event for the given session.
     *
     * @param service
     *            the Vaadin service whose session lock is being acquired or
     *            released, not {@code null}
     * @param session
     *            the Vaadin session the lock belongs to, or {@code null} if it
     *            is not known
     */
    protected AbstractSessionLockEvent(VaadinService service,
            VaadinSession session) {
        super(service);
        this.session = session;
    }

    /**
     * Gets the Vaadin service from which this event originates.
     *
     * @return the Vaadin service instance
     */
    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    /**
     * Gets the Vaadin service from which this event originates.
     *
     * @return the Vaadin service instance
     */
    public VaadinService getService() {
        return getSource();
    }

    /**
     * Gets the Vaadin session whose lock is acquired or released.
     * <p>
     * The session is not known when the lock is taken for a new HTTP session
     * before the Vaadin session has been created in it. It may also not be
     * known after the Vaadin session has been removed from the HTTP session,
     * since the lock does not keep a removed session in memory.
     *
     * @return the Vaadin session, or an empty optional if it is not known
     */
    public Optional<VaadinSession> getSession() {
        return Optional.ofNullable(session);
    }
}
