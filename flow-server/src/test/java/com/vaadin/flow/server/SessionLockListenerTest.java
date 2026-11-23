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
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    public void lockInterruptiblyAndTimedTryLock_reportOutermostHoldWithTimes()
            throws Exception {
        MockVaadinServletService service = new MockVaadinServletService();
        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        VaadinSession session = bindNewSession(service, lock);
        List<AbstractSessionLockEvent> events = recordEvents(service);

        lock.lockInterruptibly();
        lock.lockInterruptibly();
        lock.unlock();
        lock.unlock();
        assertTrue(lock.tryLock(1, TimeUnit.SECONDS));
        assertTrue(lock.tryLock(1, TimeUnit.SECONDS));
        assertTrue(lock.tryLock());
        lock.unlock();
        lock.unlock();
        lock.unlock();

        List<Class<?>> hold = List.of(SessionLockRequestedEvent.class,
                SessionLockAcquiredEvent.class, SessionLockReleasedEvent.class);
        assertEquals(hold.size() * 2, events.size());
        for (int i = 0; i < events.size(); i++) {
            AbstractSessionLockEvent event = events.get(i);
            assertEquals(hold.get(i % hold.size()), event.getClass());
            assertSame(session, event.getSession().orElseThrow());
            if (event instanceof SessionLockAcquiredEvent acquired) {
                assertFalse(acquired.getWaitTime().isNegative());
            } else if (event instanceof SessionLockReleasedEvent released) {
                assertFalse(released.getHoldTime().isNegative());
            }
        }
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

    @Test
    public void lockAcquired_javaScriptInvocationsLeftFromEarlierHold_warningLogged() {
        MockVaadinServletService service = new MockVaadinServletService();
        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        VaadinSession session = bindNewSession(service, lock);
        Logger logger = mock(Logger.class);
        when(logger.isWarnEnabled()).thenReturn(true);
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class, CALLS_REAL_METHODS)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(
                    "com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil"))
                    .thenReturn(logger);

            lock.lock();
            try {
                MockUI ui = new MockUI(session);
                ui.doInit(null, 1, "foo");
                session.addUI(ui);
                for (int i = 0; i < 1000; i++) {
                    ui.getPage().executeJs("window.foo = $0", i);
                }
            } finally {
                lock.unlock();
            }
            verify(logger, never()).warn(anyString());

            // Nothing is sent without a request or push, so the invocations
            // are still waiting the next time the lock is acquired
            lock.lock();
            lock.unlock();

            verify(logger).warn(contains(
                    "1000 JavaScript invocations scheduled for this UI have not been sent to the browser yet. "
                            + "The newest one still waiting was scheduled for node id=1"));
        } finally {
            CurrentInstance.clearAll();
        }
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
