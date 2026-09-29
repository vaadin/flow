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
 * when a Vaadin session lock has been released, for the outermost release only.
 * It is fired in reverse registration order so that listeners nest: a listener
 * added first is notified of the release last.
 *
 * @see AbstractSessionLockEvent
 * @since 25.3
 */
public class SessionLockReleasedEvent extends AbstractSessionLockEvent {

    private final Duration holdTime;

    /**
     * Creates a new event without a session and with a zero hold time.
     *
     * @param service
     *            the Vaadin service whose session lock is released, not
     *            {@code null}
     */
    public SessionLockReleasedEvent(VaadinService service) {
        this(service, null, Duration.ZERO);
    }

    /**
     * Creates a new event for the given session and hold time.
     *
     * @param service
     *            the Vaadin service whose session lock is released, not
     *            {@code null}
     * @param session
     *            the Vaadin session the lock belongs to, or {@code null} if it
     *            is not known
     * @param holdTime
     *            how long the thread held the lock, not {@code null}
     */
    public SessionLockReleasedEvent(VaadinService service,
            VaadinSession session, Duration holdTime) {
        super(service, session);
        this.holdTime = holdTime;
    }

    /**
     * Gets how long the thread held the lock, from the moment it acquired the
     * lock until it released it. The time spent in listeners of
     * {@link SessionLockAcquiredEvent} is included, since the lock is held
     * while they run.
     *
     * @return the hold time, not {@code null}
     */
    public Duration getHoldTime() {
        return holdTime;
    }
}
