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

/**
 * Event fired to {@link SessionLockListener}s around acquisition and release of
 * a Vaadin session lock.
 * <p>
 * The same lock instance protects a session whether it is acquired by the
 * framework while handling a request or via {@link VaadinSession#lock()} (for
 * example from {@link com.vaadin.flow.component.UI#access(Command)}). A
 * listener receives {@link SessionLockListener#lockRequested},
 * {@link SessionLockListener#lockAcquired} and
 * {@link SessionLockListener#lockReleased} on the same thread for a given
 * outermost lock-hold, so timing state can be kept in a thread local.
 *
 * @see SessionLockListener
 */
public class SessionLockEvent extends EventObject {

    /**
     * Creates a new session lock event.
     *
     * @param service
     *            the Vaadin service whose session lock is being acquired or
     *            released, not {@code null}
     */
    public SessionLockEvent(VaadinService service) {
        super(service);
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
}
