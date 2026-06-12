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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.shared.Registration;

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

        Assert.assertEquals(List.of("requested", "acquired", "released"),
                listener.events);
    }

    @Test
    public void tryLockSucceeds_firesRequestedAcquiredReleased() {
        MockVaadinServletService service = new MockVaadinServletService();
        RecordingListener listener = new RecordingListener();
        service.addSessionLockListener(listener);

        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        Assert.assertTrue(lock.tryLock());

        lock.unlock();

        Assert.assertEquals(List.of("requested", "acquired", "released"),
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

        Assert.assertFalse(lock.tryLock());
        Assert.assertTrue(listener.events.isEmpty());

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

        Assert.assertEquals(List.of("requested", "acquired", "released"),
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

        Assert.assertEquals(List.of("acquired-first", "acquired-second",
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

        Assert.assertTrue(listener.events.isEmpty());
    }
}
