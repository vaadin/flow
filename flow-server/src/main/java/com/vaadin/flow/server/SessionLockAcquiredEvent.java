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

import java.time.Duration;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when a Vaadin session lock has been acquired, for the outermost acquisition
 * only.
 *
 * @see AbstractSessionLockEvent
 * @since 25.3
 */
public class SessionLockAcquiredEvent extends AbstractSessionLockEvent {

    private final Duration waitTime;

    /**
     * Creates a new event without a session and with a zero wait time.
     *
     * @param service
     *            the Vaadin service whose session lock is acquired, not
     *            {@code null}
     */
    public SessionLockAcquiredEvent(VaadinService service) {
        this(service, null, Duration.ZERO);
    }

    /**
     * Creates a new event for the given session and wait time.
     *
     * @param service
     *            the Vaadin service whose session lock is acquired, not
     *            {@code null}
     * @param session
     *            the Vaadin session the lock belongs to, or {@code null} if it
     *            is not known
     * @param waitTime
     *            how long the thread waited for the lock, not {@code null}
     */
    public SessionLockAcquiredEvent(VaadinService service,
            VaadinSession session, Duration waitTime) {
        super(service, session);
        this.waitTime = waitTime;
    }

    /**
     * Gets how long the thread waited for the lock, from the moment it
     * requested the lock until it acquired it. The time other threads held the
     * lock meanwhile is included, the time spent in listeners of
     * {@link SessionLockRequestedEvent} is not.
     *
     * @return the wait time, not {@code null}
     */
    public Duration getWaitTime() {
        return waitTime;
    }
}
