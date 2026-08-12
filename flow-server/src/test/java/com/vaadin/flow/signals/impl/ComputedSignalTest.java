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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.MissingSignalUsageException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.AbstractSharedSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;
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
    void cached_updateNotificationVsLastListenerDisposal_noDeadlock()
            throws Exception {
        // Regression test for #25166: a lock-order inversion between the
        // CachedSignal monitor and a dependency's SignalTree lock.
        //
        // Committing thread: SharedValueSignal.set commits under the tree lock
        // and, while still holding it, re-runs an effect that reads a cached
        // signal. Registering that usage enters the CachedSignal monitor, so
        // the committing thread wants the monitor while holding the tree lock.
        //
        // Disposer thread: disposing the last other listener of the cached
        // signal runs the un-count callback, which (before the fix) removed the
        // dependency registration -- acquiring the tree lock -- while holding
        // the CachedSignal monitor. Opposite acquire orders form a deadlock.
        var list = new SharedListSignal<>(String.class);
        var child = list.insertLast("initial").signal();

        // Like a typical derived list: depends on the list STRUCTURE only
        // (peek does not track), so a child update does not invalidate it.
        Signal<List<String>> cached = Signal.cached(Signal.computed(() -> list
                .get().stream().map(SharedValueSignal::peek).toList()));

        // Listener B subscribes to the cached signal only. Disposing it while
        // effect A is mid-revalidation makes it the last active listener, so
        // the un-count path takes monitor -> tree lock.
        Effect listenerB = new Effect(cached::get, Runnable::run);

        var insideNotification = new CountDownLatch(1);
        var effectARuns = new AtomicInteger();

        // Effect A depends on the child value directly (so an update re-runs it
        // inline on the committing thread, under the tree lock) and on the
        // cached signal (so the re-run wants the CachedSignal monitor).
        new Effect(() -> {
            if (effectARuns.incrementAndGet() == 2) {
                // Now running on the committing thread while it holds the tree
                // lock. Give the disposer time to enter the CachedSignal
                // monitor before re-reading the cached signal.
                insideNotification.countDown();
                sleepQuietly(300);
            }
            child.get();
            cached.get();
        }, Runnable::run);

        AtomicReference<Throwable> disposerFailure = new AtomicReference<>();
        var disposer = new Thread(() -> {
            try {
                assertTrue(insideNotification.await(10, TimeUnit.SECONDS));
                listenerB.dispose();
            } catch (Throwable t) {
                disposerFailure.set(t);
            }
        }, "effect-disposer");
        disposer.setDaemon(true);

        AtomicReference<Throwable> committerFailure = new AtomicReference<>();
        // The commit runs on its own thread: with the bug the committing thread
        // deadlocks inside set() too, so the test thread must stay free to
        // observe the hang instead of blocking on the commit itself.
        var committer = new Thread(() -> {
            try {
                child.set("changed");
            } catch (Throwable t) {
                committerFailure.set(t);
            }
        }, "effect-committer");
        committer.setDaemon(true);

        disposer.start();
        committer.start();

        committer.join(10_000);
        disposer.join(10_000);

        if (committer.isAlive() || disposer.isAlive()) {
            ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
            long[] deadlocked = tmx.findDeadlockedThreads();
            StringBuilder sb = new StringBuilder(
                    "Lock-order inversion between the SignalTree lock and the "
                            + "CachedSignal monitor (see #25166). committer alive="
                            + committer.isAlive() + ", disposer alive="
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
        if (committerFailure.get() != null) {
            throw new AssertionError("Committer thread failed",
                    committerFailure.get());
        }
        if (disposerFailure.get() != null) {
            throw new AssertionError("Disposer thread failed",
                    disposerFailure.get());
        }
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
        Signal<@Nullable Object> signal = Signal
                .<@Nullable Object> cached(() -> {
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

        Signal<@Nullable String> signal = Signal
                .<@Nullable String> cached((() -> {
                    dependency.get();
                    other.set("update");
                    return null;
                }));

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
