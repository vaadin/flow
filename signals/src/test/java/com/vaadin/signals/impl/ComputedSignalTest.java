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
package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.AbstractSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.ValueSignal;

public class ComputedSignalTest extends SignalTestBase {

    @Test
    void value_constantCallback_runOnceAndConstantSignalValue() {
        AtomicInteger count = new AtomicInteger();
        Signal<Object> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return null;
        });

        assertNull(signal.value());
        assertEquals(1, count.intValue());

        signal.value();
        assertEquals(1, count.intValue());
    }

    @Test
    void value_readSignal_runLazily() {
        ValueSignal<String> source = new ValueSignal<>("value");

        ArrayList<String> invocations = new ArrayList<>();

        Signal<String> signal = Signal.computed(() -> {
            String value = source.value();
            invocations.add(value);
            return value;
        });

        assertEquals(List.of(), invocations);

        assertEquals("value", signal.value());
        assertEquals(List.of("value"), invocations);

        source.value("update");
        assertEquals(List.of("value"), invocations);

        assertEquals("update", signal.value());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void value_noOpChange_notRunAgain() {
        ValueSignal<String> source = new ValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return source.value();
        });

        signal.value();
        assertEquals(1, count.intValue());

        source.value(source.value());

        signal.value();
        assertEquals(1, count.intValue());
    }

    @Test
    void callback_updateSignal_throws() {
        ValueSignal<String> source = new ValueSignal<>("value");

        Signal<String> signal = Signal.computed((() -> {
            assertThrows(IllegalStateException.class, () -> {
                source.value("update");
            });
            return null;
        }));

        // Trigger running the callback
        signal.value();
    }

    @Test
    void effect_changeComputedDependency_effectRunAgain() {
        ValueSignal<String> source = new ValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return source.value();
        });

        ArrayList<String> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(1, count.get());
        assertEquals(List.of("value"), invocations);

        source.value("update");

        assertEquals(2, count.get());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void effect_noOpChangeInComputedDependency_effectNotRunAgainButRemainsActive() {
        ValueSignal<String> source = new ValueSignal<>("value1");
        AtomicInteger count = new AtomicInteger();

        Signal<Integer> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return source.value().length();
        });

        ArrayList<Integer> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(1, count.get());
        assertEquals(List.of(6), invocations);

        source.value("value2");

        assertEquals(2, count.get());
        assertEquals(List.of(6), invocations);

        source.value("value");
        assertEquals(3, count.get());
        assertEquals(List.of(6, 5), invocations);
    }

    @Test
    void effect_signalUpdatedInTransaction_effectIsUpdated() {
        ValueSignal<String> source = new ValueSignal<>("value");

        AtomicInteger computeCount = new AtomicInteger();
        Signal<String> signal = Signal.computed(() -> {
            computeCount.incrementAndGet();
            return source.value();
        });

        ArrayList<String> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        Signal.runInTransaction(() -> {
            source.value("update");
        });

        assertEquals(2, computeCount.intValue());
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void effect_closedEffect_computedGarbageCollected() {
        ValueSignal<String> source = new ValueSignal<>("value");

        Signal<String> signal = Signal.computed(() -> source.value());

        ArrayList<String> invocations = new ArrayList<>();

        // Explicit class to allow capturing a reference without making the
        // variable effectively final
        class CapturingRunnable implements Runnable {
            private final Signal<String> signal;

            CapturingRunnable(Signal<String> signal) {
                this.signal = signal;
            }

            @Override
            public void run() {
                invocations.add(signal.value());
            }
        }

        Signal.effect(new CapturingRunnable(signal)).run();

        assertEquals(List.of("value"), invocations);

        WeakReference<Object> ref = new WeakReference<Object>(signal);
        signal = null;

        assertTrue(waitForGarbageCollection(ref));
    }

    @Test
    void transaction_readInCommittedTransaction_notCoumptedAgainAfterTransaction() {
        ValueSignal<String> source = new ValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return source.value();
        });

        signal.value();
        assertEquals(1, count.get());

        Transaction.runInTransaction(() -> {
            source.value("update");

            signal.value();
            assertEquals(2, count.get());
        });

        signal.value();
        assertEquals(2, count.get());
    }

    @Test
    void transaction_readInAbortedTransaction_notCoumptedAgainAfterTransaction() {
        ValueSignal<String> source = new ValueSignal<>("value");
        AtomicInteger count = new AtomicInteger();

        Signal<String> signal = Signal.computed(() -> {
            count.incrementAndGet();
            return source.value();
        });

        signal.value();
        assertEquals(1, count.get());

        Transaction.runInTransaction(() -> {
            source.value("update");

            signal.value();
            assertEquals(2, count.get());

            source.verifyValue("other");
        });

        signal.value();
        assertEquals(2, count.get());
    }

    @Test
    void unsuppotedOperations_runOperations_throws() {
        AbstractSignal<Object> signal = (AbstractSignal<Object>) Signal
                .computed(() -> null);

        assertThrows(UnsupportedOperationException.class, () -> {
            signal.peek();
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            signal.peekConfirmed();
        });
    }

    @Test
    void lambda_computesValue_computedNotCached() {
        ValueSignal<Integer> signal = new ValueSignal<>(1);

        AtomicInteger count = new AtomicInteger();

        Signal<Integer> doubled = () -> {
            count.incrementAndGet();
            return signal.value() * 2;
        };

        assertEquals(2, doubled.value());
        assertEquals(1, count.intValue());

        assertEquals(2, doubled.value());
        assertEquals(2, count.intValue());

        signal.value(3);
        assertEquals(2, count.intValue());

        assertEquals(6, doubled.value());
        assertEquals(3, count.intValue());
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
