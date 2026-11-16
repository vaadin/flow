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
package com.vaadin.flow.spring.scopes;

import com.vaadin.flow.server.VaadinSession;

/**
 * Runs scope bookkeeping with the session lock held.
 * <p>
 * With {@link VaadinSession#isUILockingEnabled() UI locking}, UI events such as
 * detach are fired while holding only the UI lock, but the bean stores of a
 * session are shared by all of its UIs. Acquiring the session lock from a UI
 * thread is allowed since threads holding the session lock never wait for a UI
 * lock.
 * <p>
 * For internal use only.
 */
final class SessionLockSupport {

    private SessionLockSupport() {
        // utility class
    }

    /**
     * Runs the task with the session lock held, acquiring it if the current
     * thread does not hold it yet.
     *
     * @param session
     *            the session to lock, not {@code null}
     * @param task
     *            the task to run, not {@code null}
     */
    static void runLocked(VaadinSession session, Runnable task) {
        if (session.hasLock()) {
            task.run();
            return;
        }
        session.getLockInstance().lock();
        try {
            task.run();
        } finally {
            session.getLockInstance().unlock();
        }
    }
}
