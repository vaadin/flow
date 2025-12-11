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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.ListSignal;
import com.vaadin.signals.MapSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.ValueSignal;
import com.vaadin.signals.ValueSignalTest.AsyncValueSignal;
import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.impl.UsageTracker.Usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EffectTest extends SignalTestBase {

    @Test
    void newEffect_actionIsRunOnce() {
        AtomicInteger count = new AtomicInteger();

        Signal.effect(() -> {
            count.incrementAndGet();
        });

        assertEquals(1, count.get());
    }

    @Test
    void newEffect_closeImmediately_actionIsRunOnce() {
        AtomicInteger count = new AtomicInteger();

        Signal.effect(() -> {
            count.incrementAndGet();
        }).cleanup();

        assertEquals(1, count.get());
    }

    @Test
    void changeTracking_effectReadsValue_effectRunAgain() {
        ValueSignal<String> signal = new ValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(List.of(""), invocations);

        signal.value("update");
        assertEquals(List.of("", "update"), invocations);

        signal.value("again");
        assertEquals(List.of("", "update", "again"), invocations);
    }

    @Test
    void changeTracking_changeListStructure_effectRunAgain() {
        ListSignal<String> signal = new ListSignal<>(String.class);
        ArrayList<Integer> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value().size());
        });

        assertEquals(List.of(0), invocations);

        ValueSignal<String> child = signal.insertLast("one").signal();
        assertEquals(List.of(0, 1), invocations);

        signal.remove(child);
        assertEquals(List.of(0, 1, 0), invocations);
    }

    @Test
    void changeTracking_changeMapStructure_effectRunAgain() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        ArrayList<Integer> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value().size());
        });

        assertEquals(List.of(0), invocations);

        signal.put("key", "value");
        assertEquals(List.of(0, 1), invocations);

        signal.remove("key");
        assertEquals(List.of(0, 1, 0), invocations);
    }

    @Test
    void changeTracking_effectStopsReadingValue_effectNotRunAgain() {
        ValueSignal<String> signal = new ValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();
        AtomicBoolean read = new AtomicBoolean(true);

        Signal.effect(() -> {
            if (read.get()) {
                invocations.add(signal.value());
            } else {
                invocations.add("ignored");
            }
        });

        read.set(false);
        signal.value("update");
        assertEquals(List.of("", "ignored"), invocations);

        read.set(true);
        signal.value("again");
        assertEquals(List.of("", "ignored"), invocations,
                "The effect should no longer depend on the signal");
    }

    @Test
    void changeTracking_effectReadsThrougUntracked_effectNotRunAgain() {
        ValueSignal<String> signal = new ValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();
        AtomicBoolean read = new AtomicBoolean(true);

        Signal.effect(() -> {
            if (read.get()) {
                invocations.add(signal.value());
            } else {
                invocations.add(Signal.untracked(() -> {
                    return "untracked: " + signal.value();
                }));
            }
        });

        read.set(false);
        signal.value("update");
        assertEquals(List.of("", "untracked: update"), invocations);

        read.set(true);
        signal.value("again");
        assertEquals(List.of("", "untracked: update"), invocations,
                "The effect should no longer depend on the signal");
    }

    @Test
    void changeTracking_failedWrite_effectNotRunAgain() {
        ValueSignal<String> signal = new ValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        signal.replace("foo", "bar");
        assertEquals(List.of(""), invocations);
    }

    @Test
    void changeTracking_multipleWritesInTransaction_effectRunOnce() {
        ValueSignal<String> signal = new ValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        Signal.runInTransaction(() -> {
            signal.value("first");
            signal.value("second");
        });

        assertEquals(List.of("", "second"), invocations);
    }

    @Test
    void changeTracking_multipleSignalsInTransaction_effectRunOnce() {
        ValueSignal<String> signal1 = new ValueSignal<>("");
        ValueSignal<String> signal2 = new ValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal1.value() + signal2.value());
        });

        Signal.runInTransaction(() -> {
            signal1.value("one ");
            signal2.value("two");
        });

        assertEquals(List.of("", "one two"), invocations);
    }

    @Test
    void changeTracking_changeOtherPartOfNode_effectNotRunAgain() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(List.of("value"), invocations);

        signal.asNode().putChildWithValue("key", "value");
        assertEquals(List.of("value"), invocations);
    }

    @Test
    void changeTracking_asyncSignal_effectUsesSubmittedValue() {
        AsyncValueSignal signal = new AsyncValueSignal();
        signal.value("");
        signal.tree().confirmSubmitted();

        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(List.of(""), invocations);

        signal.replace("", "update");
        assertEquals(List.of("", "update"), invocations);

        signal.tree()
                .confirm(List.of(TestUtil.writeRootValueCommand("conflict")));
        assertEquals(List.of("", "update", "conflict"), invocations);

        signal.tree().confirmSubmitted();
        assertEquals(List.of("", "update", "conflict"), invocations);
    }

    @Test
    void changeTracking_noOpChange_effectNotRunButRemainsActive() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(List.of("value"), invocations);

        signal.value("value");
        assertEquals(List.of("value"), invocations);

        signal.value("update");
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void changeTracking_readChildNodes_coveredByNextEffectInvocation() {
        ListSignal<String> signal = new ListSignal<>(String.class);
        ArrayList<List<String>> invocations = new ArrayList<>();

        Signal.effect(() -> {
            List<String> values = signal.value().stream().map(Signal::value)
                    .toList();
            invocations.add(values);
        });

        assertEquals(List.of(List.of()), invocations);

        signal.insertLast("One");
        assertEquals(List.of(List.of(), List.of("One")), invocations);

        signal.insertLast("Two");
        assertEquals(List.of(List.of(), List.of("One"), List.of("One", "Two")),
                invocations);
    }

    @Test
    void changeTracking_changeValueToNull_effectTriggered() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(Arrays.asList("initial"), invocations);

        signal.value(null);
        assertEquals(Arrays.asList("initial", null), invocations);
    }

    @Test
    void changeTracking_lambdaSignal_changeTracked() {
        ValueSignal<Integer> signal = new ValueSignal<>(1);
        Signal<Integer> doubled = () -> signal.value() * 2;

        ArrayList<Integer> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(doubled.value());
        });

        assertEquals(List.of(2), invocations);

        signal.value(2);
        assertEquals(List.of(2, 4), invocations);
    }

    @Test
    void close_effectReadsValue_affectNotRunAfterClose() {
        ArrayList<String> invocations = new ArrayList<>();
        ValueSignal<String> signal = new ValueSignal<>("");

        CleanupCallback closer = Signal.effect(() -> {
            invocations.add(signal.value());
        });

        closer.cleanup();
        signal.value("update");

        assertEquals(List.of(""), invocations);
    }

    @Test
    void dispatcher_multipleWrites_singleUpdateWhenDispatcherTriggers() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        TestExecutor dispatcher = useTestEffectDispatcher();

        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);

        signal.value("update1");
        signal.value("update2");
        assertEquals(List.of("initial"), invocations);

        dispatcher.runPendingTasks();
        assertEquals(List.of("initial", "update2"), invocations);
    }

    @Test
    void dispatcher_closeWithPendingUpdate_noUpdate() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        TestExecutor dispatcher = useTestEffectDispatcher();

        ArrayList<String> invocations = new ArrayList<>();

        CleanupCallback closer = Signal.effect(() -> {
            invocations.add(signal.value());
        });
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);

        signal.value("update");
        assertEquals(List.of("initial"), invocations);

        closer.cleanup();
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsException_effectRemainsFunctional() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        RuntimeException exception = new RuntimeException("Expected exception");

        ArrayList<String> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
            throw exception;
        });
        assertUncaughtException(exception);

        signal.value("update");

        assertUncaughtException(exception);
        assertEquals(List.of("initial", "update"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsException_otherEffectsWork() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        RuntimeException exception = new RuntimeException("Expected exception");
        Signal.effect(() -> {
            throw exception;
        });

        assertUncaughtException(exception);

        ArrayList<String> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        signal.value("update");
        assertEquals(List.of("initial", "update"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsError_effectClosed() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        ArrayList<String> invocations = new ArrayList<>();
        Error error = new Error("Expected error");
        Signal.effect(() -> {
            invocations.add(signal.value());

            throw error;
        });
        assertEquals(List.of("initial"), invocations);
        assertUncaughtException(caught -> caught.getCause() == error);

        signal.value("update");
        assertEquals(List.of("initial"), invocations);
    }

    @Test
    void infiniteLoopDetection_writeUnrelatedSignal_noError() {
        ValueSignal<String> other = new ValueSignal<>("other");
        ValueSignal<String> signal = new ValueSignal<>("signal");

        Signal.effect(() -> {
            other.value(signal.value());
        });
        assertEquals("signal", other.value());

        signal.value("update");
        assertEquals("update", other.value());
    }

    @Test
    void infiniteLoopDetection_writeOwnSignal_loopDetected() {
        ValueSignal<String> signal = new ValueSignal<>("signal");
        ValueSignal<String> trigger = new ValueSignal<>("trigger");

        AtomicInteger count = new AtomicInteger();

        Signal.effect(() -> {
            count.incrementAndGet();
            trigger.value();
            signal.value();

            assertThrows(IllegalStateException.class, () -> {
                signal.value("update");
            });
        });

        assertEquals(1, count.get());

        trigger.value("update");

        assertEquals(1, count.get(), "Signal should have been disabled");
    }

    @Test
    void infiniteLoopDetection_loopBetweenEffects_loopDetectedFromSetter() {
        ValueSignal<String> signal1 = new ValueSignal<>("signal");
        ValueSignal<String> signal2 = new ValueSignal<>("signal");

        AtomicInteger throwCount = new AtomicInteger();

        Signal.effect(() -> {
            String value = signal2.value() + " update";

            try {
                signal1.value(value);
            } catch (IllegalStateException e) {
                throwCount.incrementAndGet();
            }
        });
        assertEquals(0, throwCount.get(),
                "Should not fail with only one effect active");

        Signal.effect(() -> {
            signal2.value(signal1.value() + " update");
        });
        assertEquals(1, throwCount.get(),
                "Should fail when the other effect is created");
    }

    @Test
    void infiniteLoopDetection_loopBetweenConditionalEffects_loopDetected() {
        ValueSignal<String> signal1 = new ValueSignal<>("signal");
        ValueSignal<String> signal2 = new ValueSignal<>("signal");
        ValueSignal<Boolean> trigger = new ValueSignal<>(false);

        Signal.effect(() -> {
            signal1.value(signal2.value() + " update");
        });

        Signal.effect(() -> {
            if (trigger.value()) {
                signal2.value(signal1.value() + " update");
            }
        });
        assertNoUncaughtException();

        trigger.value(true);
        assertUncaughtException(IllegalStateException.class);
    }

    @Test
    void infiniteLoopDetection_loopBetweenAsyncEffects_loopDetected() {
        TestExecutor dispatcher = useTestEffectDispatcher();

        ValueSignal<String> signal1 = new ValueSignal<>("signal");
        ValueSignal<String> signal2 = new ValueSignal<>("signal");

        Signal.effect(() -> {
            signal1.value(signal2.value() + " update");
        });
        dispatcher.runPendingTasks();

        Signal.effect(() -> {
            signal2.value(signal1.value() + " update");
        });
        // Runs the 2nd effect which schedules running the 1st effect
        dispatcher.runPendingTasks();
        assertNoUncaughtException();

        // Runs the 1st effect which is where the loop can be detected
        dispatcher.runPendingTasks();
        assertUncaughtException(IllegalStateException.class);
    }

    @Test
    void infiniteLoopDetection_concurrentSignalWrite_notDetectedAsLoop() {
        TestExecutor dispatcher = useTestEffectDispatcher();
        List<String> invocations = new ArrayList<>();

        ValueSignal<String> signal = new ValueSignal<>("signal") {
            @Override
            protected Usage createUsage(Transaction transaction) {
                Usage usage = super.createUsage(transaction);

                return new Usage() {
                    @Override
                    public boolean hasChanges() {
                        return usage.hasChanges();
                    }

                    @Override
                    public CleanupCallback onNextChange(TransientListener listener) {
                        /*
                         * Emulate race condition by injecting an unrelated
                         * write at the moment when the effect starts listening
                         * for changes
                         */
                        value("update");

                        return usage.onNextChange(listener);
                    }
                };
            }
        };

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        dispatcher.runPendingTasks();
        assertEquals(List.of("signal"), invocations);

        dispatcher.runPendingTasks();
        assertEquals(List.of("signal", "update"), invocations);
    }
}
