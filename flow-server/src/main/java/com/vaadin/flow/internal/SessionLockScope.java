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
package com.vaadin.flow.internal;

import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Holds the lock of a session and restores the current instances when the lock
 * is released, for use with try-with-resources.
 * <p>
 * Code that sets the current UI for work it does with the session lock held
 * uses this so that no code runs with a current UI without holding the lock of
 * its session: the current instances that were in place before locking are
 * restored right after unlocking, also when unlocking fails. The session stays
 * current until it is unlocked, as the access tasks that unlocking runs rely on
 * it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class SessionLockScope implements AutoCloseable {

    private final VaadinSession session;
    private final Map<Class<?>, CurrentInstance> instances;

    private SessionLockScope(VaadinSession session,
            Map<Class<?>, CurrentInstance> instances) {
        this.session = session;
        this.instances = instances;
    }

    /**
     * Locks the given session, remembering the current instances so that
     * {@link #close()} can restore them.
     *
     * @param session
     *            the session to lock, not <code>null</code>
     * @return the scope that unlocks the session when closed, not
     *         <code>null</code>
     */
    public static SessionLockScope lock(VaadinSession session) {
        Map<Class<?>, CurrentInstance> instances = CurrentInstance
                .getInstances();
        session.lock();
        return new SessionLockScope(session, instances);
    }

    /**
     * Locks the given session and makes the given UI current until the scope is
     * closed.
     *
     * @param session
     *            the session to lock, not <code>null</code>
     * @param ui
     *            the UI to make current, or <code>null</code> to have no
     *            current UI while the session is locked
     * @return the scope that unlocks the session when closed, not
     *         <code>null</code>
     */
    public static SessionLockScope lock(VaadinSession session, UI ui) {
        SessionLockScope scope = lock(session);
        UI.setCurrent(ui);
        return scope;
    }

    /**
     * Unlocks the session and then makes the instances that were current before
     * locking the only current instances, also when unlocking fails.
     */
    @Override
    public void close() {
        try {
            session.unlock();
        } finally {
            CurrentInstance.clearAll();
            CurrentInstance.restoreInstances(instances);
        }
    }
}
