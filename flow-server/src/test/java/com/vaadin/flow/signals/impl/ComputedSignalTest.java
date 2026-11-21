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
package com.vaadin.flow.signals.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.MissingSignalUsageException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.AbstractSharedSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/*
 * The original computed signal was also a cached signal. For historical
 * reasons, this class tests both cached signals and the "new" computed signals
 * that do no caching.
 */
class ComputedSignalTest extends SignalTestBase {

    @Test
    void cached_treeLockHolderRegistersVsLastListenerDisposal_noDeadlock()
            throws Exception {
        // Regression test for #25166: a lock-order inversion between the
        // CachedSignal leaf lock and a dependency's SignalTree lock.
        //
        // Lock holder: a thread holding the dependency's tree lock registers a
        // new listener on the cached signal, which enters the CachedSignal
        // leaf lock. This tree lock -> leaf lock order is allowed.
        //
        // Disposer thread: disposing the last other listener of the cached
        // signal runs the un-count callback, which (before the fix) removed the
        // dependency registration -- acquiring the tree lock -- while holding
        // the leaf lock. Opposite acquire orders form a deadlock.
        //
        // Observers are no longer notified while a tree lock is held, so a
        // commit cannot put the committing thread in the lock holder's place.
        // The test holds the tree lock explicitly instead, so that it keeps
        // guarding the CachedSignal side of the lock order.
        class TreeLockSignal extends SharedValueSignal<String> {
            TreeLockSignal() {
                super("initial");
            }

            ReentrantLock treeLock() {
                return tree().getLock();
            }
        }
        var dependency = new TreeLockSignal();
        Signal<String> cached = Signal.cached(dependency.map(v -> v + "!"));

        // Listener B is the only listener of the cached signal, so disposing
        // it takes the un-count path: leaf lock -> (before the fix) tree lock.
        Effect listenerB = new Effect(cached::get, Runnable::run);

        var treeLockHeld = new CountDownLatch(1);

        AtomicReference<Throwable> disposerFailure = new AtomicReference<>();
        var disposer = new Thread(() -> {
            try {
                assertTrue(treeLockHeld.await(10, TimeUnit.SECONDS));
                listenerB.dispose();
            } catch (Throwable t) {
                disposerFailure.set(t);
            }
        }, "effect-disposer");
        disposer.setDaemon(true);

        AtomicReference<Throwable> lockHolderFailure = new AtomicReference<>();
        // The lock holder runs on its own thread: with the bug it deadlocks
        // too, so the test thread must stay free to observe the hang.
        var lockHolder = new Thread(() -> {
            ReentrantLock treeLock = dependency.treeLock();
            treeLock.lock();
            try {
                // Give the disposer time to enter the CachedSignal leaf lock
                // before registering a new listener on the cached signal.
                treeLockHeld.countDown();
                sleepQuietly(300);
                new Effect(cached::get, Runnable::run).dispose();
            } catch (Throwable t) {
                lockHolderFailure.set(t);
            } finally {
                treeLock.unlock();
            }
        }, "tree-lock-holder");
        lockHolder.setDaemon(true);

        disposer.start();
        lockHolder.start();

        lockHolder.join(10_000);
        disposer.join(10_000);

        if (lockHolder.isAlive() || disposer.isAlive()) {
            ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
            long[] deadlocked = tmx.findDeadlockedThreads();
            StringBuilder sb = new StringBuilder(
                    "Lock-order inversion between the SignalTree lock and the "
                            + "CachedSignal leaf lock (see #25166). lock holder alive="
                            + lockHolder.isAlive() + ", disposer alive="
                            + disposer.isAlive());
            if (deadlocked != null) {
                sb.append(", deadlocked thread IDs=")
                        .append(Arrays.toString(deadlocked));
                for (var info : tmx.getThreadInfo(deadlocked, true, true)) {
                    sb.append("\n  ").append(info.getThreadName())
                            .append(" waiting on ").append(info.getLockName())
                            .append(" owned by ")
                            .append(info.getLockOwnerName());
                }
            }
            fail(sb.toString());
        }
        if (lockHolderFailure.get() != null) {
            throw new AssertionError("Lock holder thread failed",
                    lockHolderFailure.get());
        }
        if (disposerFailure.get() != null) {
            throw new AssertionError("Disposer thread failed",
                    disposerFailure.get());
        }
    }

    @Test
    void cached_sharedUpdateVsConcurrentRead_noDeadlock() throws Exception {
        // Regression test for #26130: a lock-order inversion between the
        // shared signal's tree lock and the cached signal's own tree lock.
        //
        // Writer: SharedValueSignal.update notifies observers under the shared
        // tree lock, and the cached signal's dependency observer then reads
        // the cached signal's tree.
        //
        // Reader: reading the cached signal submits the refreshed value to its
        // own tree, whose observer re-runs the effect under that tree lock,
        // and the effect then reads the shared signal's tree.
        var shared = new SharedValueSignal<>(0);
        Signal<Integer> cached = Signal.cached(shared.map(value -> value + 1));
        AtomicReference<Integer> effectValue = new AtomicReference<>();
        Effect effect = new Effect(() -> effectValue.set(cached.get()),
                Runnable::run);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        var writer = startLoop("cached-writer",
                () -> shared.update(value -> value + 1), failure);
        var reader = startLoop("cached-reader", cached::peek, failure);

        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        long[] deadlocked = null;
        for (int i = 0; i < 40 && deadlocked == null; i++) {
            Thread.sleep(50); // NOSONAR lets the threads race between checks
            deadlocked = tmx.findDeadlockedThreads();
        }

        writer.interrupt();
        reader.interrupt();
        if (deadlocked == null) {
            writer.join(10_000);
            reader.join(10_000);
        }

        assertNull(deadlocked,
                "Lock-order inversion between two SignalTree locks (see #26130)");
        if (failure.get() != null) {
            throw new AssertionError("Worker thread failed", failure.get());
        }

        // The effect keeps being notified after the concurrent updates
        shared.set(-1);
        assertEquals(0, effectValue.get());
        effect.dispose();
    }

    private static Thread startLoop(String name, Runnable step,
            AtomicReference<Throwable> failure) {
        var thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    step.run();
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void cached_constantCallback_throws() {
        Signal<String> signal = Signal.cached(() -> "const");
        assertThrows(MissingSignalUsageException.class, signal::peek);
    }

    @Test
    void cached_constantCallback_runOnceAndConstantSignalValue() {
        var dependency = createDependency();
        AtomicInteger count = new AtomicInteger();
        Signal<@Nullable Object> signal = Signal.cached(() -> {
            dependency.get();
            count.incrementAndGet();
            return null;
        });

        assertNull(signal.peek());
        assertEquals(1, count.intValue());

        signal.peek();
        assertEquals(1, count.intValue());
    }

    @Test
    void cached_readSignal_runLazily() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        ArrayList<String> invocations = new ArrayList<>();

        Signal<String> signal = Signal.cached(() -> {
            String value = source.get();
            invocations.add(value);
            return value;
        });

        assertEquals(List.of(), invocations);

        assertEquals("value", signal.peek());
        assertEquals(List.of("value"), invocations);

        source.set("update");
        assertEquals(List.of("value"), invocations);

        assertEquals("update", signal.peek());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void computed_readSignal_runLazily() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        ArrayList<String> invocations = new ArrayList<>();

        Signal<String> signal = Signal.computed(() -> {
            String value = source.get();
            invocations.add(value);
            return value;
        });

        assertEquals(List.of(), invocations);

        assertEquals("value", signal.peek());
        assertEquals(List.of("value"), invocations);

        source.set("update");
        assertEquals(List.of("value"), invocations);

        assertEquals("update", signal.peek());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void computed_noChange_runAgain() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return source.get();
        });

        signal.peek();
        assertEquals(1, count.intValue());

        signal.peek();
        assertEquals(2, count.intValue());
    }

    @Test
    void cached_noOpChange_notRunAgain() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.cached(() -> {
            count.incrementAndGet();
            return source.get();
        });

        signal.peek();
        assertEquals(1, count.intValue());

        source.set(source.peek());

        signal.peek();
        assertEquals(1, count.intValue());
    }

    @Test
    void map_mapCachedSignal_valueIsMapped() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        Signal<Integer> cached = Signal.cached(() -> source.get().length());

        Signal<Integer> doubled = cached.map(l -> l * 2);

        assertEquals(10, doubled.peek());
    }

    @Test
    void map_mapMappedSignal_valueIsMapped() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        Signal<Integer> computed = source.map(String::length);

        Signal<Integer> doubled = computed.map(l -> l * 2);

        assertEquals(10, doubled.peek());
    }

    @Test
    void map_countCallbackInvocations_invocationsAreNotCached() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<Integer> computed = source.map(value -> {
            count.incrementAndGet();
            return value.length();
        });
        assertEquals(0, count.get());

        computed.peek();
        assertEquals(1, count.get());

        computed.peek();
        assertEquals(2, count.get());
    }

    @Test
    void not_booleanInputs_negatedOutputs() {
        SharedValueSignal<Boolean> signal = new SharedValueSignal<>(
                Boolean.TRUE);
        Signal<Boolean> negated = Signal.not(signal);

        assertFalse(negated.peek());

        signal.set(false);
        assertTrue(negated.peek());
    }

    @Test
    void callback_updateOtherSignal_signalUpdated() {
        var dependency = createDependency();
        SharedValueSignal<String> other = new SharedValueSignal<>("value");

        Signal<@Nullable String> signal = Signal.cached(() -> {
            dependency.get();
            other.set("update");
            return null;
        });

        // Trigger running the callback
        signal.peek();

        assertEquals("update", other.peek());
    }

    @Test
    void effect_changeComputedDependency_effectRunAgain() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.cached(() -> {
            count.incrementAndGet();
            return source.get();
        });

        ArrayList<String> invocations = new ArrayList<>();
        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        assertEquals(1, count.get());
        assertEquals(List.of("value"), invocations);

        source.set("update");

        assertEquals(2, count.get());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void effect_noOpChangeInComputedDependency_effectNotRunAgainButRemainsActive() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value1");
        AtomicInteger count = new AtomicInteger();

        Signal<Integer> signal = Signal.cached(() -> {
            count.incrementAndGet();
            return source.get().length();
        });

        ArrayList<Integer> invocations = new ArrayList<>();
        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        assertEquals(1, count.get());
        assertEquals(List.of(6), invocations);

        source.set("value2");

        assertEquals(2, count.get());
        assertEquals(List.of(6), invocations);

        source.set("value");
        assertEquals(3, count.get());
        assertEquals(List.of(6, 5), invocations);
    }

    @Test
    void effect_signalUpdatedInTransaction_effectIsUpdated() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        AtomicInteger computeCount = new AtomicInteger();
        Signal<String> signal = Signal.cached(() -> {
            computeCount.incrementAndGet();
            return source.get();
        });

        ArrayList<String> invocations = new ArrayList<>();
        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        Signal.runInTransaction(() -> {
            source.set("update");
        });

        assertEquals(2, computeCount.intValue());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void effect_closedEffect_cachedGarbageCollected() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        Signal<String> signal = Signal.cached(() -> source.get());

        ArrayList<String> invocations = new ArrayList<>();

        // Explicit class to allow capturing a reference without making the
        // variable effectively final
        class CapturingRunnable implements EffectAction {
            private final Signal<String> signal;

            CapturingRunnable(Signal<String> signal) {
                this.signal = signal;
            }

            @Override
            public void execute() {
                invocations.add(signal.get());
            }
        }

        Signal.unboundEffect(new CapturingRunnable(signal)).remove();

        assertEquals(List.of("value"), invocations);

        WeakReference<Object> ref = new WeakReference<Object>(signal);
        signal = null;

        assertTrue(waitForGarbageCollection(ref));
    }

    @Test
    void transaction_readInCommittedTransaction_notCoumptedAgainAfterTransaction() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.cached(() -> {
            count.incrementAndGet();
            return source.get();
        });

        signal.peek();
        assertEquals(1, count.get());

        Transaction.runInTransaction(() -> {
            source.set("update");

            signal.get();
            assertEquals(2, count.get());
        });

        signal.peek();
        assertEquals(2, count.get());
    }

    @Test
    void transaction_readInAbortedTransaction_valueRestoredAfterRejection() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.cached(() -> {
            count.incrementAndGet();
            return source.get();
        });

        assertEquals("value", signal.peek());
        assertEquals(1, count.get());

        Transaction.runInTransaction(() -> {
            source.set("update");

            assertEquals("update", signal.get());
            assertEquals(2, count.get());

            source.verifyValue("other");
        });

        /*
         * Count is 3 because the cached signal's dependency was captured with
         * the in-transaction value ("update"). After the rejected transaction,
         * the submitted value is still "value", which differs from the captured
         * value, so the cached signal must recompute.
         */
        assertEquals("value", signal.peek());
        assertEquals(3, count.get());
    }

    @Test
    void lambda_getLocalValueSignalExplicitTransaction_doNotThrow() {
        var shared = new SharedValueSignal<>(1);
        var local = new ValueSignal<>(2);

        Signal<Integer> computed = () -> shared.get() + local.get();

        AtomicInteger count = new AtomicInteger();
        Signal.unboundEffect(() -> {
            count.set(computed.get());
        });

        assertEquals(3, count.get());

        // ValueSignal update should not throw IllegalStateException.
        // Update runs in an explicit transaction.
        shared.update(x -> x + 1);
        assertEquals(4, count.get());
        // Verify that set also works
        shared.set(shared.peek() + 1);
        assertEquals(5, count.get());
    }

    @Test
    void cached_getLocalValueSignalExplicitTransaction_doNotThrow() {
        var shared = new SharedValueSignal<>(1);
        var local = new ValueSignal<>(2);

        Signal<Integer> cached = Signal
                .cached(() -> shared.get() + local.get());

        AtomicInteger count = new AtomicInteger();
        Signal.unboundEffect(() -> {
            count.set(cached.get());
        });

        assertEquals(3, count.get());

        // ValueSignal update should not throw IllegalStateException.
        // Update runs in an explicit transaction.
        shared.update(x -> x + 1);
        assertEquals(4, count.get());
        // Verify that set also works
        shared.set(shared.peek() + 1);
        assertEquals(5, count.get());
    }

    @Test
    void lambda_getLocalListSignalExplicitTransaction_doNotThrow() {
        var shared = new SharedValueSignal<>(1);
        var local = new ListSignal<Integer>();
        local.insertFirst(2);

        Signal<Integer> computed = () -> shared.get()
                + local.get().get(0).get();

        AtomicInteger count = new AtomicInteger();
        Signal.unboundEffect(() -> {
            count.set(computed.get());
        });

        assertEquals(3, count.get());

        // ListSignal update should not throw IllegalStateException.
        // Update runs in an explicit transaction.
        shared.update(x -> x + 1);
        assertEquals(4, count.get());
        // Verify that set also works
        shared.set(shared.peek() + 1);
        assertEquals(5, count.get());
    }

    @Test
    void cached_getLocalListSignalExplicitTransaction_doNotThrow() {
        var shared = new SharedValueSignal<>(1);
        var local = new ListSignal<Integer>();
        local.insertFirst(2);

        Signal<Integer> cached = Signal
                .cached(() -> shared.get() + local.get().get(0).peek());

        AtomicInteger count = new AtomicInteger();
        Signal.unboundEffect(() -> {
            count.set(cached.get());
        });

        assertEquals(3, count.get());

        // ListSignal update should not throw IllegalStateException.
        // Update runs in an explicit transaction.
        shared.update(x -> x + 1);
        assertEquals(4, count.get());
        // Verify that set also works
        shared.set(shared.peek() + 1);
        assertEquals(5, count.get());
    }

    @Test
    void unsuppotedOperations_runOperations_throws() {
        AbstractSharedSignal<Object> signal = (AbstractSharedSignal<Object>) Signal
                .cached(() -> null);

        assertThrows(UnsupportedOperationException.class, () -> {
            signal.peekConfirmed();
        });
    }

    @Test
    void lambda_computesValue_computedNotCached() {
        SharedValueSignal<Integer> signal = new SharedValueSignal<>(1);

        AtomicInteger count = new AtomicInteger();

        Signal<Integer> doubled = () -> {
            count.incrementAndGet();
            return signal.get() * 2;
        };

        assertEquals(2, doubled.peek());
        assertEquals(1, count.intValue());

        assertEquals(2, doubled.peek());
        assertEquals(2, count.intValue());

        signal.set(3);
        assertEquals(2, count.intValue());

        assertEquals(6, doubled.peek());
        assertEquals(3, count.intValue());
    }

    @Test
    void exceptionHandling_callbackThrows_rethrowWhenReading() {
        SharedValueSignal<Boolean> shouldThrow = new SharedValueSignal<>(false);

        AtomicInteger count = new AtomicInteger();
        Signal<Boolean> cached = Signal.cached(() -> {
            count.incrementAndGet();
            if (shouldThrow.get()) {
                throw new RuntimeException("Expected exception");
            } else {
                return shouldThrow.get();
            }
        });
        assertFalse(cached.peek());
        assertEquals(1, count.get());

        shouldThrow.set(true);
        assertThrows(RuntimeException.class, () -> cached.peek());
        assertEquals(2, count.get());

        assertThrows(RuntimeException.class, () -> cached.peek());
        assertEquals(2, count.get(), "Exception should be cached");

        shouldThrow.set(false);
        assertFalse(cached.peek());
        assertEquals(3, count.get());
    }

    private static boolean waitForGarbageCollection(WeakReference<?> ref) {
        long deadline = System.nanoTime() + Duration.ofMillis(100).toNanos();
        while (System.nanoTime() < deadline) {
            System.gc();

            if (ref.get() == null) {
                return true;
            }

            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        }

        return false;
    }
}
