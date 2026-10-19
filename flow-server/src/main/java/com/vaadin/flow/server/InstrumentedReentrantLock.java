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

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.EventObject;
import java.util.concurrent.TimeUnit;
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
 * signalled.
 * <p>
 * The lock is created before the {@link VaadinSession} it protects, and is
 * stored in the HTTP session next to it. The session binds itself with
 * {@link #bind(VaadinService, VaadinSession)} whenever it is stored or loaded.
 * The lock keeps the session only through a weak reference, since the lock
 * stays in the HTTP session after the Vaadin session has been removed from it.
 * The service and the session are {@code transient}; after session
 * passivation/activation the lock behaves as a plain {@link ReentrantLock}
 * until the session binds itself again.
 * <p>
 * Every acquisition path is instrumented so events stay balanced regardless of
 * how the lock was taken: {@link #lock()} blocks and so reports
 * {@code lockRequested} before the attempt, while {@link #lockInterruptibly()}
 * and {@link #tryLock()} / {@link #tryLock(long, TimeUnit)} may fail and so
 * report {@code lockRequested} and {@code lockAcquired} together only on a
 * successful outermost acquisition. The wait time is measured around the
 * attempt on every path. This matters because
 * {@link VaadinService#ensureAccessQueuePurged(VaadinSession)} acquires the
 * lock with {@code tryLock} and then {@link #unlock() unlocks}; instrumenting
 * only {@code lock()} would leave that path firing {@code lockReleased} with no
 * matching acquire. {@code lockReleased} is only fired for a hold whose
 * acquisition had a listener for it, which also keeps the events balanced when
 * the service is bound while the lock is held.
 */
class InstrumentedReentrantLock extends ReentrantLock {

    // Both references are only ever replaced as a whole, never mutated, so
    // volatile is all it takes to publish them to other threads
    @SuppressWarnings("java:S3077")
    private transient volatile VaadinService service;

    @SuppressWarnings("java:S3077")
    private transient volatile WeakReference<VaadinSession> session;

    /*
     * Only accessed by the thread holding the lock.
     */
    private transient boolean holdTimed;
    private transient long acquiredAtNanos;

    InstrumentedReentrantLock(VaadinService service) {
        this.service = service;
    }

    /**
     * Binds the lock to the session it protects, and to its service again after
     * deserialization.
     *
     * @param service
     *            the service the session belongs to
     * @param session
     *            the session this lock protects
     */
    void bind(VaadinService service, VaadinSession session) {
        this.service = service;
        WeakReference<VaadinSession> current = this.session;
        if (current == null || current.get() != session) {
            this.session = new WeakReference<>(session);
        }
    }

    @Override
    public void lock() {
        if (getHoldCount() > 0) {
            super.lock();
            return;
        }
        fireLockRequested();
        boolean timeWait = hasListener(SessionLockAcquiredEvent.class);
        long requestedAt = timeWait ? System.nanoTime() : 0;
        super.lock();
        lockAcquired(false, timeWait, requestedAt);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (getHoldCount() > 0) {
            super.lockInterruptibly();
            return;
        }
        boolean timeWait = hasListener(SessionLockAcquiredEvent.class);
        long requestedAt = timeWait ? System.nanoTime() : 0;
        // Report after a confirmed acquisition: lockInterruptibly may abort
        // with InterruptedException without taking the lock.
        super.lockInterruptibly();
        lockAcquired(true, timeWait, requestedAt);
    }

    @Override
    public boolean tryLock() {
        if (getHoldCount() > 0) {
            return super.tryLock();
        }
        boolean timeWait = hasListener(SessionLockAcquiredEvent.class);
        long requestedAt = timeWait ? System.nanoTime() : 0;
        boolean acquired = super.tryLock();
        if (acquired) {
            lockAcquired(true, timeWait, requestedAt);
        }
        return acquired;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (getHoldCount() > 0) {
            return super.tryLock(timeout, unit);
        }
        boolean timeWait = hasListener(SessionLockAcquiredEvent.class);
        long requestedAt = timeWait ? System.nanoTime() : 0;
        boolean acquired = super.tryLock(timeout, unit);
        if (acquired) {
            lockAcquired(true, timeWait, requestedAt);
        }
        return acquired;
    }

    @Override
    public void unlock() {
        VaadinService currentService = service;
        boolean ultimateRelease = getHoldCount() == 1;
        boolean fireReleased = ultimateRelease && holdTimed
                && currentService != null;
        // Read before unlocking, another thread may take the lock right after
        long heldNanos = fireReleased ? System.nanoTime() - acquiredAtNanos : 0;
        if (ultimateRelease) {
            holdTimed = false;
        }
        super.unlock();
        if (fireReleased) {
            // Released is fired in reverse registration order so that the
            // listeners nest: a listener notified first of the acquisition is
            // notified last of the release
            currentService.getEventBus().fireEventInReverseOrder(
                    new SessionLockReleasedEvent(currentService, getSession(),
                            Duration.ofNanos(heldNanos)));
        }
    }

    /*
     * Locking is hot enough that the events are only built, and the times only
     * measured, when somebody is there to receive them: an application that
     * observes nothing pays for nothing.
     */

    private void lockAcquired(boolean fireRequested, boolean timeWait,
            long requestedAt) {
        holdTimed = hasListener(SessionLockReleasedEvent.class);
        long acquiredAt = (timeWait || holdTimed) ? System.nanoTime() : 0;
        acquiredAtNanos = acquiredAt;
        if (fireRequested) {
            fireLockRequested();
        }
        VaadinService currentService = service;
        if (timeWait && currentService != null) {
            currentService.getEventBus()
                    .fireEvent(new SessionLockAcquiredEvent(currentService,
                            getSession(),
                            Duration.ofNanos(acquiredAt - requestedAt)));
        }
    }

    private void fireLockRequested() {
        VaadinService currentService = service;
        if (currentService != null
                && hasListener(SessionLockRequestedEvent.class)) {
            currentService.getEventBus()
                    .fireEvent(new SessionLockRequestedEvent(currentService,
                            getSession()));
        }
    }

    private boolean hasListener(Class<? extends EventObject> eventType) {
        VaadinService currentService = service;
        VaadinServiceEventBus eventBus = currentService == null ? null
                : currentService.getEventBus();
        return eventBus != null && eventBus.hasListener(eventType);
    }

    private VaadinSession getSession() {
        WeakReference<VaadinSession> current = session;
        return current == null ? null : current.get();
    }
}
