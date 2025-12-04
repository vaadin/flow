/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals;

import java.util.ConcurrentModificationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ReferenceSignalTest extends SignalTestBase {

    @Test
    void constructor_noArgs_nullValue() {
        ReferenceSignal<Object> signal = new ReferenceSignal<>();

        assertNull(signal.value());
    }

    @Test
    void constructor_initialValue_initialValueUsed() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("value");

        assertEquals("value", signal.value());
    }

    @Test
    void setValue_valueUsed() {
        ReferenceSignal<String> signal = new ReferenceSignal<>();
        signal.value("value");

        assertEquals("value", signal.value());
    }

    @Test
    void setValue_oldValueInResult() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        SignalOperation<String> operation = signal.value("update");

        String resultValue = TestUtil.assertSuccess(operation);
        assertEquals("initial", resultValue);
    }

    @Test
    void replace_expectedValue_valueUpdated() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        SignalOperation<Void> operation = signal.replace("initial", "update");

        TestUtil.assertSuccess(operation);
        assertEquals("update", signal.value());
    }

    @Test
    void replace_otherValue_valueNotUpdated() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        SignalOperation<Void> operation = signal.replace("other", "update");

        TestUtil.assertFailure(operation);
        assertEquals("initial", signal.value());
    }

    @Test
    void update_updatesTheValue() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        CancelableOperation<String> operation = signal.update(oldValue -> {
            assertEquals("initial", oldValue);
            return "update";
        });

        String oldValue = TestUtil.assertSuccess(operation);
        assertEquals("initial", oldValue);

        assertEquals("update", signal.value());
    }

    @Test
    void update_callbackThrows_exceptionPropagated() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        RuntimeException theException = new RuntimeException();

        RuntimeException caught = assertThrows(RuntimeException.class, () -> {
            signal.update(ignore -> {
                throw theException;
            });
        });

        assertSame(theException, caught);
    }

    @Test
    void modify_modifiesValue_valueModified() {
        String[] holder = new String[] { "initial" };
        ReferenceSignal<String[]> signal = new ReferenceSignal<>(holder);

        signal.modify(value -> {
            assertSame(holder, value);
            holder[0] = "update";
        });

        assertEquals("update", holder[0]);
        assertSame(holder, signal.value());
    }

    @Test
    void asReadonly_notWritable() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");
        Signal<String> readonly = signal.asReadonly();

        assertFalse(readonly instanceof WritableSignal<String>);
    }

    @Test
    void asReadonly_changeSignal_readonlyUpdated() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");
        Signal<String> readonly = signal.asReadonly();

        signal.value("update");
        assertEquals("update", readonly.value());
    }

    @Test
    void usageTracker_setNewValue_changeDetected() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        assertFalse(usage.hasChanges());

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.value("update");

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
    }

    @Test
    void usageTracker_updateSameValue_noChangeDetected() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.update(x -> x);

        assertFalse(usage.hasChanges());
        assertFalse(invoked.get());
    }

    @Test
    void usageTracker_listenToChangedUsage_initialFlagSet() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.value("update");

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertTrue(initial);
            invoked.set(true);
            return false;
        });

        assertTrue(invoked.get());
    }

    @Test
    void usageTracker_keepListening_listenerKept() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.value("update1");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return true;
        });

        // Verify preserving after initial and trigger subsequent update
        signal.value("update2");
        assertEquals(2, count.get());

        // Verify subsequent update
        signal.value("update3");
        assertEquals(3, count.get());
    }

    @Test
    void usageTracker_stopAfterInitial_stopped() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.value("update1");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return false;
        });

        assertEquals(1, count.intValue());

        signal.value("update2");
        assertEquals(1, count.intValue());
    }

    @Test
    void usageTracker_stopAfterSubsequent_stopped() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return false;
        });

        signal.value("update1");
        assertEquals(1, count.intValue());

        signal.value("update2");
        assertEquals(1, count.intValue());
    }

    @Test
    void usageTracker_anyModify_detectedAsAChange() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.modify(value -> {
        });
        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_peek_noUsageDetected() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.peek();
        });

        assertSame(UsageTracker.NO_USAGE, usage);
    }

    @Test
    void concurrency_updateHoldsLock() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        signal.update(value -> {
            assertTrue(signal.lock.isHeldByCurrentThread());
            return value;
        });
        assertFalse(signal.lock.isHeldByCurrentThread());
    }

    @Test
    void concurrency_lockHeld_operationsAreBlocked() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");
        signal.lock.lock();

        AtomicInteger completed = new AtomicInteger();

        Thread.startVirtualThread(() -> {
            signal.value();
            completed.incrementAndGet();
        });

        Thread.startVirtualThread(() -> {
            signal.peek();
            completed.incrementAndGet();
        });

        Thread.startVirtualThread(() -> {
            signal.value("update");
            completed.incrementAndGet();
        });

        Thread.startVirtualThread(() -> {
            signal.replace("foo", "bar");
            completed.incrementAndGet();
        });

        Thread.startVirtualThread(() -> {
            signal.update(x -> x);
            completed.incrementAndGet();
        });

        // Wait for all threads to start
        assertEventually(() -> signal.lock.getQueueLength() == 5);
        assertEquals(0, completed.get());

        signal.lock.unlock();

        // Wait for all threads to complete
        assertEventually(() -> completed.get() == 5);
        assertEquals(0, signal.lock.getQueueLength());
    }

    @Test
    void concurrency_modifyWhileLocked_modifyThrowsEagerly()
            throws InterruptedException {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Thread lockThread = Thread.startVirtualThread(() -> signal.lock.lock());
        // Wait until locked
        lockThread.join();

        assertThrows(ConcurrentModificationException.class, () -> {
            signal.modify(x -> {
                fail("Should never get here");
            });
        });
    }

    @Test
    void concurrency_otherUsageWhileModifying_otherUsageThrows() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        Semaphore modifyStarted = new Semaphore(0);
        Semaphore modifyCanProceed = new Semaphore(0);

        // Modify on another thread
        Thread.startVirtualThread(() -> {
            signal.modify(value -> {
                modifyStarted.release();
                // Block until all assertions are done
                modifyCanProceed.acquireUninterruptibly();
            });
        });

        // Wait until other thread is inside the modify method
        modifyStarted.acquireUninterruptibly();

        assertThrows(ConcurrentModificationException.class,
                () -> signal.value());
        assertThrows(ConcurrentModificationException.class,
                () -> signal.peek());
        assertThrows(ConcurrentModificationException.class,
                () -> signal.value("update"));
        assertThrows(ConcurrentModificationException.class,
                () -> signal.replace("foo", "bar"));
        assertThrows(ConcurrentModificationException.class,
                () -> signal.update(x -> x));
        assertThrows(ConcurrentModificationException.class,
                () -> signal.modify(x -> {
                }));

        modifyCanProceed.release();
    }

    @Test
    void transactions_readSignalInTransaction_throws() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.value();
            });
        });
    }

    @Test
    void transactions_writeSignalInTransaction_throws() {
        ReferenceSignal<String> signal = new ReferenceSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.value("update");
            });
        });
    }

    private static void assertEventually(BooleanSupplier test) {
        for (int i = 0; i < 10; i++) {
            if (test.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fail();
    }
}
