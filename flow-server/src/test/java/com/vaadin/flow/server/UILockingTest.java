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

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UILockingTest {

    private static final long TIMEOUT_SECONDS = 5;

    private MockVaadinSession session;
    private UI ui1;
    private UI ui2;

    @BeforeEach
    void setUp() {
        session = new MockVaadinSession(new MockVaadinServletService());
        session.applyLockStrategy(SessionLockCheckStrategy.THROW);
        session.applyUILocking(true);
        ui1 = addUI(0);
        ui2 = addUI(1);
    }

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    void accessSynchronously_holdsOnlyUILock() {
        AtomicBoolean ran = new AtomicBoolean();
        ui1.accessSynchronously(() -> {
            assertTrue(ui1.getInternals().hasUILock());
            assertFalse(session.hasLock());
            assertFalse(ui2.getInternals().hasUILock());
            assertSame(ui1, UI.getCurrent());
            ui1.getElement().setAttribute("foo", "bar");
            ran.set(true);
        });
        assertTrue(ran.get());
        assertFalse(ui1.getInternals().hasUILock());
    }

    @Test
    void uiStateWithSessionLockOnly_throws() {
        session.lock();
        try {
            assertThrows(IllegalStateException.class,
                    () -> ui1.getElement().setAttribute("foo", "bar"));
        } finally {
            session.unlock();
        }
    }

    @Test
    void sessionStateAccessibleWithUILock() {
        ui1.accessSynchronously(() -> {
            assertTrue(session.hasAccess());
            session.setAttribute("foo", "bar");
            assertEquals("bar", session.getAttribute("foo"));
            assertEquals(2, session.getUIs().size());
        });
        assertFalse(session.hasAccess());
    }

    @Test
    void busyUI_doesNotBlockOtherUIOrSession() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = holdUI(ui1, locked, release);
        try {
            assertTrue(locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            // Neither the other UI nor the session is blocked
            AtomicBoolean ran = new AtomicBoolean();
            ui2.accessSynchronously(() -> ran.set(true));
            assertTrue(ran.get());

            assertTrue(session.getLockInstance().tryLock(TIMEOUT_SECONDS,
                    TimeUnit.SECONDS));
            session.getLockInstance().unlock();
        } finally {
            release.countDown();
            holder.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
    }

    @Test
    void access_busyUI_runsWhenUIIsReleased() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = holdUI(ui1, locked, release);
        Future<Void> future;
        AtomicReference<Thread> taskThread = new AtomicReference<>();
        try {
            assertTrue(locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            future = ui1.access(() -> {
                assertTrue(ui1.getInternals().hasUILock());
                taskThread.set(Thread.currentThread());
            });
            assertFalse(future.isDone());
        } finally {
            release.countDown();
            holder.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
        future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Run by the thread that released the UI lock
        assertSame(holder, taskThread.get());
    }

    @Test
    void access_freeUI_runsImmediately() throws Exception {
        Future<Void> future = ui1
                .access(() -> assertTrue(ui1.getInternals().hasUILock()));
        future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(future.isDone());
        assertFalse(ui1.getInternals().hasUILock());
    }

    @Test
    void accessSynchronously_busyUIWhileHoldingSessionLock_throws()
            throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = holdUI(ui1, locked, release);
        try {
            assertTrue(locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            session.lock();
            try {
                // Waiting here could deadlock, so it is refused
                assertThrows(IllegalStateException.class,
                        () -> ui1.accessSynchronously(() -> {
                        }));
            } finally {
                session.unlock();
            }
        } finally {
            release.countDown();
            holder.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
    }

    @Test
    void accessSynchronously_freeUIWhileHoldingOtherUILock_runs() {
        AtomicBoolean ran = new AtomicBoolean();
        ui1.accessSynchronously(() -> ui2.accessSynchronously(() -> {
            assertTrue(ui1.getInternals().hasUILock());
            assertTrue(ui2.getInternals().hasUILock());
            ran.set(true);
        }));
        assertTrue(ran.get());
    }

    @Test
    void uiThreadMayLockSession() {
        AtomicBoolean ran = new AtomicBoolean();
        ui1.accessSynchronously(() -> {
            session.lock();
            try {
                assertTrue(session.hasLock());
                ran.set(true);
            } finally {
                session.unlock();
            }
        });
        assertTrue(ran.get());
    }

    @Test
    void setLocale_busyUIIsUpdatedWhenReleased() throws Exception {
        Locale locale = Locale.GERMAN.equals(ui1.getLocale()) ? Locale.FRENCH
                : Locale.GERMAN;
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = holdUI(ui1, locked, release);
        try {
            assertTrue(locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            session.lock();
            try {
                session.setLocale(locale);
            } finally {
                session.unlock();
            }
            // The free UI is updated right away, the busy one later
            assertEquals(locale, ui2.getLocale());
            assertNotEquals(locale, ui1.getLocale());
        } finally {
            release.countDown();
            holder.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
        assertEquals(locale, ui1.getLocale());
    }

    @Test
    void findUIWithoutLock_returnsUIOfSession() {
        assertSame(ui1, session.findUIWithoutLock(ui1.getUIId()));
        assertSame(ui2, session.findUIWithoutLock(ui2.getUIId()));
        assertEquals(2, session.getUIsWithoutLock().size());
    }

    @Test
    void lockForUI_locksOnlyTheUI() {
        Runnable release = VaadinSession.lockForUI(session, ui1);
        try {
            assertTrue(ui1.getInternals().hasUILock());
            assertFalse(session.hasLock());
        } finally {
            release.run();
        }
        assertFalse(ui1.getInternals().hasUILock());

        release = VaadinSession.lockForUI(session, null);
        try {
            assertTrue(session.hasLock());
        } finally {
            release.run();
        }
    }

    @Test
    void sessionLocking_uiLockNotUsed() {
        MockVaadinSession sessionLocked = new MockVaadinSession(
                new MockVaadinServletService());
        assertFalse(sessionLocked.isUILockingEnabled());

        UI ui = new TestUI(0);
        sessionLocked.lock();
        try {
            ui.getInternals().setSession(sessionLocked);
            sessionLocked.addUI(ui);
        } finally {
            sessionLocked.unlock();
        }

        ui.accessSynchronously(() -> {
            assertTrue(sessionLocked.hasLock());
            assertFalse(ui.getInternals().hasUILock());
        });
    }

    /**
     * Adds a UI the same way bootstrap does: with the session lock and the lock
     * of the new UI held.
     */
    private UI addUI(int id) {
        UI ui = new TestUI(id);
        session.lock();
        try {
            assertTrue(ui.getInternals().tryLockUI());
            try {
                ui.getInternals().setSession(session);
                session.addUI(ui);
            } finally {
                ui.getInternals().unlockUI();
            }
        } finally {
            session.unlock();
        }
        return ui;
    }

    /**
     * Starts a thread that holds the lock of the given UI until released.
     */
    private static Thread holdUI(UI ui, CountDownLatch locked,
            CountDownLatch release) {
        Thread thread = new Thread(() -> ui.accessSynchronously(() -> {
            locked.countDown();
            try {
                release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        thread.start();
        return thread;
    }

    private static class TestUI extends UI {
        private final int id;

        private TestUI(int id) {
            this.id = id;
        }

        @Override
        public int getUIId() {
            return id;
        }
    }
}
