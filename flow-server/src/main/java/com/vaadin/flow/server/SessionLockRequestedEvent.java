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

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when a Vaadin session lock is about to be requested, for the outermost
 * acquisition only.
 *
 * @see AbstractSessionLockEvent
 * @since 25.3
 */
public class SessionLockRequestedEvent extends AbstractSessionLockEvent {

    /**
     * Creates a new event without a session.
     *
     * @param service
     *            the Vaadin service whose session lock is requested, not
     *            {@code null}
     */
    public SessionLockRequestedEvent(VaadinService service) {
        super(service);
    }

    /**
     * Creates a new event for the given session.
     *
     * @param service
     *            the Vaadin service whose session lock is requested, not
     *            {@code null}
     * @param session
     *            the Vaadin session the lock belongs to, or {@code null} if it
     *            is not known
     */
    public SessionLockRequestedEvent(VaadinService service,
            VaadinSession session) {
        super(service, session);
    }
}
