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

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ReentrantLock} used for Vaadin session locking that notifies the
 * owning {@link VaadinService}'s {@link SessionLockListener}s around the
 * outermost lock acquisition and release.
 * <p>
 * Both the request-handling lock path
 * ({@link VaadinService#lockSession(WrappedSession)}) and
 * {@link VaadinSession#lock()} / {@link VaadinSession#unlock()} (used by
 * {@link com.vaadin.flow.component.UI#access(Command)}) operate on the same
 * lock instance, so instrumenting the lock captures every acquisition exactly
 * once at the true acquire/release moment.
 * <p>
 * Only the outermost acquisition is reported: reentrant re-locks
 * ({@link #getHoldCount()} {@code > 0}) acquire without waiting and are not
 * signalled. The reference to the service is {@code transient}; after session
 * passivation/activation it behaves as a plain {@link ReentrantLock} until a
 * fresh instance is created.
 */
class InstrumentedReentrantLock extends ReentrantLock {

    private final transient VaadinService service;

    InstrumentedReentrantLock(VaadinService service) {
        this.service = service;
    }

    @Override
    public void lock() {
        boolean outermost = getHoldCount() == 0;
        if (outermost && service != null) {
            service.fireSessionLockRequested();
        }
        super.lock();
        if (outermost && service != null) {
            service.fireSessionLockAcquired();
        }
    }

    @Override
    public void unlock() {
        boolean ultimateRelease = getHoldCount() == 1;
        super.unlock();
        if (ultimateRelease && service != null) {
            service.fireSessionLockReleased();
        }
    }
}
