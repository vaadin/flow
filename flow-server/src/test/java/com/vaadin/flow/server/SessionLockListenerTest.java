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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import com.vaadin.flow.shared.Registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SessionLockListenerTest {

    private static final class RecordingListener
            implements SessionLockListener {
        final List<String> events = new ArrayList<>();

        @Override
        public void lockRequested(SessionLockEvent event) {
            events.add("requested");
        }

        @Override
        public void lockAcquired(SessionLockEvent event) {
            events.add("acquired");
        }

        @Override
        public void lockReleased(SessionLockEvent event) {
            events.add("released");
        }
    }

    @Test
    public void singleLockUnlock_firesRequestedAcquiredReleasedOnce() {
        MockVaadinServletService service = new MockVaadinServletService();
        RecordingListener listener = new RecordingListener();
        service.addSessionLockListener(listener);

        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        lock.lock();
        try {
            // critical section
        } finally {
            lock.unlock();
        }

        assertEquals(List.of("requested", "acquired", "released"),
                listener.events);
    }

    @Test
    public void tryLockSucceeds_firesRequestedAcquiredReleased() {
        MockVaadinServletService service = new MockVaadinServletService();
        RecordingListener listener = new RecordingListener();
        service.addSessionLockListener(listener);

        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        assertTrue(lock.tryLock());

        lock.unlock();

        assertEquals(List.of("requested", "acquired", "released"),
                listener.events);
    }

    @Test
    public void tryLockFails_firesNothing() throws Exception {
        MockVaadinServletService service = new MockVaadinServletService();
        RecordingListener listener = new RecordingListener();
        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);

        // Hold the lock from another thread so tryLock can't take it.
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            locked.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        locked.await();
        // Register the listener after the lock is held, so the
        // holder's own acquisition is not recorded and the list can only grow
        // if the failing tryLock wrongly emits an event.
        service.addSessionLockListener(listener);

        assertFalse(lock.tryLock());
        assertTrue(listener.events.isEmpty());

        release.countDown();
        holder.join();
    }

    @Test
    public void reentrantLock_reportsOnlyOutermostHold() {
        MockVaadinServletService service = new MockVaadinServletService();
        RecordingListener listener = new RecordingListener();
        service.addSessionLockListener(listener);

        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        lock.lock();
        lock.lock();
        lock.unlock();
        lock.unlock();

        assertEquals(List.of("requested", "acquired", "released"),
                listener.events);
    }

    @Test
    public void multipleListeners_releasedFiresInReverseRegistrationOrder() {
        MockVaadinServletService service = new MockVaadinServletService();
        List<String> order = new ArrayList<>();
        service.addSessionLockListener(new SessionLockListener() {
            @Override
            public void lockAcquired(SessionLockEvent event) {
                order.add("acquired-first");
            }

            @Override
            public void lockReleased(SessionLockEvent event) {
                order.add("released-first");
            }
        });
        service.addSessionLockListener(new SessionLockListener() {
            @Override
            public void lockAcquired(SessionLockEvent event) {
                order.add("acquired-second");
            }

            @Override
            public void lockReleased(SessionLockEvent event) {
                order.add("released-second");
            }
        });

        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        lock.lock();
        try {
            // critical section
        } finally {
            lock.unlock();
        }

        assertEquals(List.of("acquired-first", "acquired-second",
                "released-second", "released-first"), order);
    }

    @Test
    public void removedListener_isNotNotified() {
        MockVaadinServletService service = new MockVaadinServletService();
        RecordingListener listener = new RecordingListener();
        Registration registration = service.addSessionLockListener(listener);
        registration.remove();

        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        lock.lock();
        lock.unlock();

        assertTrue(listener.events.isEmpty());
    }

    @Test
    public void lockAfterWaiting_reportsSessionWaitTimeAndHoldTime()
            throws Exception {
        MockVaadinServletService service = new MockVaadinServletService();
        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        VaadinSession session = bindNewSession(service, lock);

        // Hold the lock from another thread until this thread has waited for
        // it for a while.
        CountDownLatch locked = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                locked.countDown();
                while (!lock.hasQueuedThreads()) {
                    Thread.onSpinWait();
                }
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        locked.await();
        // Listeners added while the holder has the lock are not notified of
        // the release of that hold, since its acquisition was not observed.
        List<AbstractSessionLockEvent> events = recordEvents(service);

        lock.lock();
        try {
            Thread.sleep(20);
        } finally {
            lock.unlock();
        }
        holder.join();

        assertEquals(List.of(SessionLockRequestedEvent.class,
                SessionLockAcquiredEvent.class, SessionLockReleasedEvent.class),
                events.stream().map(Object::getClass).toList());
        events.forEach(
                event -> assertSame(session, event.getSession().orElseThrow()));
        Duration waitTime = ((SessionLockAcquiredEvent) events.get(1))
                .getWaitTime();
        Duration holdTime = ((SessionLockReleasedEvent) events.get(2))
                .getHoldTime();
        assertTrue(waitTime + " should be at least 20ms",
                waitTime.compareTo(Duration.ofMillis(20)) >= 0);
        assertTrue(holdTime + " should be at least 20ms",
                holdTime.compareTo(Duration.ofMillis(20)) >= 0);
    }

    @Test
    public void deserializedLock_reportsNothingUntilSessionIsBound() {
        MockVaadinServletService service = new MockVaadinServletService();
        InstrumentedReentrantLock lock = SerializationUtils
                .roundtrip(new InstrumentedReentrantLock(service));
        List<AbstractSessionLockEvent> events = recordEvents(service);

        // The service is bound while the lock is held, the release of that
        // hold is not reported either
        VaadinSession session = bindNewSession(service, lock);
        assertTrue(events.isEmpty());

        lock.lock();
        lock.unlock();

        assertEquals(3, events.size());
        events.forEach(
                event -> assertSame(session, event.getSession().orElseThrow()));
    }

    private static VaadinSession bindNewSession(VaadinService service,
            InstrumentedReentrantLock lock) {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        when(wrappedSession.getAttribute(service.getServiceName() + ".lock"))
                .thenReturn(lock);
        VaadinSession session = new VaadinSession(service);
        lock.lock();
        try {
            session.refreshTransients(wrappedSession, service);
        } finally {
            lock.unlock();
        }
        return session;
    }

    private static List<AbstractSessionLockEvent> recordEvents(
            VaadinService service) {
        List<AbstractSessionLockEvent> events = new ArrayList<>();
        VaadinServiceEventBus eventBus = service.getEventBus();
        eventBus.addListener(SessionLockRequestedEvent.class, events::add);
        eventBus.addListener(SessionLockAcquiredEvent.class, events::add);
        eventBus.addListener(SessionLockReleasedEvent.class, events::add);
        return events;
    }
}
