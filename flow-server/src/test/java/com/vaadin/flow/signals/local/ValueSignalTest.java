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
package com.vaadin.flow.signals.local;

import java.util.ConcurrentModificationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.SerializableBiPredicate;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.impl.UsageTracker;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ValueSignalTest extends SignalTestBase {

    @Test
    void constructor_initialValue_initialValueUsed() {
        ValueSignal<String> signal = new ValueSignal<>("value");

        assertEquals("value", signal.get());
    }

    @Test
    void setValue_valueUsed() {
        ValueSignal<String> signal = new ValueSignal<>("");
        signal.set("value");

        assertEquals("value", signal.get());
    }

    @Test
    void replace_expectedValue_valueUpdated() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        boolean result = signal.replace("initial", "update");

        assertTrue(result);
        assertEquals("update", signal.get());
    }

    @Test
    void replace_otherValue_valueNotUpdated() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        boolean result = signal.replace("other", "update");

        assertFalse(result);
        assertEquals("initial", signal.get());
    }

    @Test
    void update_updatesTheValue() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        String previousValue = signal.update(oldValue -> {
            assertEquals("initial", oldValue);
            return "update";
        });

        assertEquals("initial", previousValue);

        assertEquals("update", signal.get());
    }

    @Test
    void update_callbackThrows_exceptionPropagated() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        RuntimeException theException = new RuntimeException();

        RuntimeException caught = assertThrows(RuntimeException.class, () -> {
            signal.update(ignore -> {
                throw theException;
            });
        });

        assertSame(theException, caught);
    }

    @Test
    void modify_modifiesValue_valueModified() throws Exception {
        String[] holder = new String[] { "initial" };
        ValueSignal<String[]> signal = new ValueSignal<>(holder);

        new MockVaadinSession().runWithLock(() -> {
            signal.modify(value -> {
                assertSame(holder, value);
                holder[0] = "update";
            });

            assertEquals("update", holder[0]);
            assertSame(holder, signal.get());
            return null;
        });
    }

    @Test
    void asReadonly_notWritable() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        Signal<String> readonly = signal.asReadonly();

        assertFalse(readonly instanceof ValueSignal<String>);
    }

    @Test
    void asReadonly_changeSignal_readonlyUpdated() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        Signal<String> readonly = signal.asReadonly();

        signal.set("update");
        assertEquals("update", readonly.get());
    }

    @Test
    void usageTracker_setNewValue_changeDetected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        assertFalse(usage.hasChanges());

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.set("update");

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
    }

    @Test
    void usageTracker_updateSameValue_noChangeDetected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
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
    void update_sameValueEqualObjects_noChangeDetected() {
        ValueSignal<String> signal = new ValueSignal<>(new String("identical"));

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        // Return new instance with same value using Objects.equals
        signal.update(x -> new String("identical"));

        assertFalse(usage.hasChanges());
        assertFalse(invoked.get());
    }

    @Test
    void update_nullToNull_noChangeDetected() {
        ValueSignal<String> signal = new ValueSignal<>(null);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.update(x -> null);

        assertFalse(usage.hasChanges());
        assertFalse(invoked.get());
    }

    @Test
    void update_differentValue_changeDetected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.update(x -> "different");

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
        assertEquals("different", signal.get());
    }

    @Test
    void update_nullToDifferent_changeDetected() {
        ValueSignal<String> signal = new ValueSignal<>(null);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.update(x -> "value");

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
        assertEquals("value", signal.get());
    }

    @Test
    void update_differentToNull_changeDetected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.update(x -> null);

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
        assertNull(signal.get());
    }

    @Test
    void usageTracker_setSameValue_noChangeDetected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.set("initial");

        assertFalse(usage.hasChanges());
        assertFalse(invoked.get());
    }

    @Test
    void set_nullToNull_noChangeDetected() {
        ValueSignal<String> signal = new ValueSignal<>(null);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.set(null);

        assertFalse(usage.hasChanges());
        assertFalse(invoked.get());
    }

    @Test
    void set_equalObjects_noChangeDetected() {
        ValueSignal<String> signal = new ValueSignal<>(new String("identical"));

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.set(new String("identical"));

        assertFalse(usage.hasChanges());
        assertFalse(invoked.get());
    }

    @Test
    void usageTracker_listenToChangedUsage_initialFlagSet() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.set("update");

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
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.set("update1");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return true;
        });

        // Verify preserving after initial and trigger subsequent update
        signal.set("update2");
        assertEquals(2, count.get());

        // Verify subsequent update
        signal.set("update3");
        assertEquals(3, count.get());
    }

    @Test
    void usageTracker_stopAfterInitial_stopped() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.set("update1");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return false;
        });

        assertEquals(1, count.intValue());

        signal.set("update2");
        assertEquals(1, count.intValue());
    }

    @Test
    void usageTracker_stopAfterSubsequent_stopped() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return false;
        });

        signal.set("update1");
        assertEquals(1, count.intValue());

        signal.set("update2");
        assertEquals(1, count.intValue());
    }

    @Test
    void usageTracker_anyModify_detectedAsAChange() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.modify(value -> {
        });
        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_peek_noUsageDetected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.peek();
        });

        assertSame(UsageTracker.NO_USAGE, usage);
    }

    @Test
    void concurrency_updateHoldsLock() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        signal.update(value -> {
            assertTrue(signal.getLock().isHeldByCurrentThread());
            return value;
        });
        assertFalse(signal.getLock().isHeldByCurrentThread());
    }

    @Test
    void concurrency_lockHeld_operationsAreBlocked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        signal.getLock().lock();

        AtomicInteger completed = new AtomicInteger();

        Thread.startVirtualThread(() -> {
            signal.get();
            completed.incrementAndGet();
        });

        Thread.startVirtualThread(() -> {
            signal.peek();
            completed.incrementAndGet();
        });

        Thread.startVirtualThread(() -> {
            signal.set("update");
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
        assertEventually(() -> signal.getLock().getQueueLength() == 5);
        assertEquals(0, completed.get());

        signal.getLock().unlock();

        // Wait for all threads to complete
        assertEventually(() -> completed.get() == 5);
        assertEquals(0, signal.getLock().getQueueLength());
    }

    @Test
    void concurrency_modifyWhileLocked_modifyThrowsEagerly()
            throws InterruptedException {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Thread lockThread = Thread
                .startVirtualThread(() -> signal.getLock().lock());
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
        ValueSignal<String> signal = new ValueSignal<>("initial");

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

        assertThrows(ConcurrentModificationException.class, () -> signal.get());
        assertThrows(ConcurrentModificationException.class,
                () -> signal.peek());
        assertThrows(ConcurrentModificationException.class,
                () -> signal.set("update"));
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
        ValueSignal<String> signal = new ValueSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.get();
            });
        });
    }

    @Test
    void transactions_writeSignalInTransaction_throws() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.set("update");
            });
        });
    }

    @Test
    void transactions_peekSignalInTransaction_throws() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.peek();
            });
        });
    }

    @Test
    void transactions_replaceSignalInTransaction_throws() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.replace("initial", "update");
            });
        });
    }

    @Test
    void transactions_updateSignalInTransaction_throws() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.update(x -> "update");
            });
        });
    }

    @Test
    void transactions_modifySignalInTransaction_throws() {
        ValueSignal<String[]> signal = new ValueSignal<>(
                new String[] { "initial" });

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                signal.modify(x -> x[0] = "update");
            });
        });
    }

    @Test
    void customEqualityChecker_nullNotAllowed_throws() {
        assertThrows(NullPointerException.class,
                () -> new ValueSignal<>("initial", null));
    }

    @Test
    void customEqualityChecker_set_usesCustomChecker() {
        // Custom checker that considers all strings starting with same letter
        // as equal
        SerializableBiPredicate<String, String> checker = (a, b) -> {
            if (a == null || b == null) {
                return a == b;
            }
            return a.charAt(0) == b.charAt(0);
        };

        ValueSignal<String> signal = new ValueSignal<>("apple", checker);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            invoked.set(true);
            return false;
        });

        // "apricot" starts with 'a' like "apple", should be considered equal
        signal.set("apricot");
        assertFalse(invoked.get());
        assertEquals("apple", signal.get());

        // "banana" starts with 'b', should trigger change
        signal.set("banana");
        assertTrue(invoked.get());
        assertEquals("banana", signal.get());
    }

    @Test
    void customEqualityChecker_update_usesCustomChecker() {
        // Custom checker that uses case-insensitive comparison
        SerializableBiPredicate<String, String> checker = (a, b) -> {
            if (a == null || b == null) {
                return a == b;
            }
            return a.equalsIgnoreCase(b);
        };

        ValueSignal<String> signal = new ValueSignal<>("Hello", checker);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            invoked.set(true);
            return false;
        });

        // "HELLO" is equal case-insensitively, should not trigger change
        signal.update(x -> "HELLO");
        assertFalse(invoked.get());
        assertEquals("Hello", signal.get());

        // "World" is different, should trigger change
        signal.update(x -> "World");
        assertTrue(invoked.get());
        assertEquals("World", signal.get());
    }

    @Test
    void customEqualityChecker_referenceEquality_triggersOnNewInstance() {
        // Custom checker that uses reference equality (==)
        SerializableBiPredicate<String, String> checker = (a, b) -> a == b;

        ValueSignal<String> signal = new ValueSignal<>("initial", checker);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            invoked.set(true);
            return false;
        });

        // Same reference, should not trigger change
        String value = signal.get();
        signal.set(value);
        assertFalse(invoked.get());

        // New instance with same value, should trigger change (reference
        // equality)
        signal.set(new String("initial"));
        assertTrue(invoked.get());
    }

    @Test
    void customEqualityChecker_replace_usesCustomChecker() {
        // Custom checker that uses case-insensitive comparison
        SerializableBiPredicate<String, String> checker = (a, b) -> {
            if (a == null || b == null) {
                return a == b;
            }
            return a.equalsIgnoreCase(b);
        };

        ValueSignal<String> signal = new ValueSignal<>("Hello", checker);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            invoked.set(true);
            return false;
        });

        // Replace with same value (case-insensitive match), should return true
        // but not trigger change
        boolean result = signal.replace("HELLO", "HELLO");
        assertTrue(result);
        assertFalse(invoked.get());
        assertEquals("Hello", signal.get());

        // Replace with different value
        result = signal.replace("HELLO", "World");
        assertTrue(result);
        assertTrue(invoked.get());
        assertEquals("World", signal.get());
    }

    @Test
    void toString_includesValue() {
        ValueSignal<String> signal = new ValueSignal<>("signal value");

        assertEquals("ValueSignal[signal value]", signal.toString());
    }

    @Test
    void threadSafety_modifyWithSessionLock_noException() throws Exception {
        MockVaadinSession session = new MockVaadinSession();

        ValueSignal<String[]> signal = new ValueSignal<>(
                new String[] { "initial" });

        session.runWithLock(() -> {
            signal.modify(value -> value[0] = "modified");
            signal.get();
            signal.set(new String[] { "new" });
            return null;
        });
    }

    @Test
    void threadSafety_modifyWithSessionThenAccessWithoutLock_throws()
            throws Exception {
        MockVaadinSession session = new MockVaadinSession();

        ValueSignal<String[]> signal = new ValueSignal<>(
                new String[] { "initial" });

        session.runWithLock(() -> {
            signal.modify(value -> value[0] = "modified");
            return null;
        });

        Thread other = Thread.startVirtualThread(() -> {
            assertThrows(IllegalStateException.class, () -> signal.get());
        });
        other.join();
    }

    @Test
    void threadSafety_modifyWithoutSessionThenAccess_throws() {
        ValueSignal<String[]> signal = new ValueSignal<>(
                new String[] { "initial" });

        signal.modify(value -> value[0] = "modified");

        assertThrows(IllegalStateException.class, () -> signal.get());
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
