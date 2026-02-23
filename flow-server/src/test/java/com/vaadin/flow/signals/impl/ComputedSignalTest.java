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

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.shared.AbstractSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComputedSignalTest extends SignalTestBase {

    @Test
    void value_constantCallback_runOnceAndConstantSignalValue() {
        AtomicInteger count = new AtomicInteger();
        Signal<Object> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return null;
        });

        assertNull(signal.peek());
        assertEquals(1, count.intValue());

        signal.peek();
        assertEquals(1, count.intValue());
    }

    @Test
    void value_readSignal_runLazily() {
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
    void value_noOpChange_notRunAgain() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.computed(() -> {
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
    void map_mapComputedSignal_valueIsMapped() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        Signal<Integer> computed = Signal.computed(() -> source.get().length());

        Signal<Integer> doubled = computed.map(l -> l * 2);

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

        signal.set(null);
        assertNull(negated.peek());
    }

    @Test
    void callback_updateOtherSignal_signalUpdated() {
        SharedValueSignal<String> other = new SharedValueSignal<>("value");

        Signal<String> signal = Signal.computed((() -> {
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

        Signal<String> signal = Signal.computed(() -> {
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

        Signal<Integer> signal = Signal.computed(() -> {
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
        Signal<String> signal = Signal.computed(() -> {
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
    void effect_closedEffect_computedGarbageCollected() {
        SharedValueSignal<String> source = new SharedValueSignal<>("value");

        Signal<String> signal = Signal.computed(() -> source.get());

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

        Signal<String> signal = Signal.computed(() -> {
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

        Signal<String> signal = Signal.computed(() -> {
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
         * Count is 3 because the computed signal's dependency was captured with
         * the in-transaction value ("update"). After the rejected transaction,
         * the submitted value is still "value", which differs from the captured
         * value, so the computed signal must recompute.
         */
        assertEquals("value", signal.peek());
        assertEquals(3, count.get());
    }

    @Test
    void unsuppotedOperations_runOperations_throws() {
        AbstractSignal<Object> signal = (AbstractSignal<Object>) Signal
                .computed(() -> null);

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
        Signal<Boolean> computed = Signal.computed(() -> {
            count.incrementAndGet();
            if (shouldThrow.get()) {
                throw new RuntimeException("Expected exception");
            } else {
                return shouldThrow.get();
            }
        });
        assertFalse(computed.peek());
        assertEquals(1, count.get());

        shouldThrow.set(true);
        assertThrows(RuntimeException.class, () -> computed.peek());
        assertEquals(2, count.get());

        assertThrows(RuntimeException.class, () -> computed.peek());
        assertEquals(2, count.get(), "Exception should be cached");

        shouldThrow.set(false);
        assertFalse(computed.peek());
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
