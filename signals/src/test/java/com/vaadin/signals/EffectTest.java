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
package com.vaadin.signals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.UsageTracker.Usage;
import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.shared.SharedListSignal;
import com.vaadin.signals.shared.SharedMapSignal;
import com.vaadin.signals.shared.SharedValueSignal;
import com.vaadin.signals.shared.SharedValueSignalTest.AsyncSharedValueSignal;

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
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(List.of(""), invocations);

        signal.set("update");
        assertEquals(List.of("", "update"), invocations);

        signal.set("again");
        assertEquals(List.of("", "update", "again"), invocations);
    }

    @Test
    void changeTracking_changeListStructure_effectRunAgain() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        ArrayList<Integer> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value().size());
        });

        assertEquals(List.of(0), invocations);

        SharedValueSignal<String> child = signal.insertLast("one").signal();
        assertEquals(List.of(0, 1), invocations);

        signal.remove(child);
        assertEquals(List.of(0, 1, 0), invocations);
    }

    @Test
    void changeTracking_changeMapStructure_effectRunAgain() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
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
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
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
        signal.set("update");
        assertEquals(List.of("", "ignored"), invocations);

        read.set(true);
        signal.set("again");
        assertEquals(List.of("", "ignored"), invocations,
                "The effect should no longer depend on the signal");
    }

    @Test
    void changeTracking_effectReadsThrougUntracked_effectNotRunAgain() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
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
        signal.set("update");
        assertEquals(List.of("", "untracked: update"), invocations);

        read.set(true);
        signal.set("again");
        assertEquals(List.of("", "untracked: update"), invocations,
                "The effect should no longer depend on the signal");
    }

    @Test
    void changeTracking_failedWrite_effectNotRunAgain() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        signal.replace("foo", "bar");
        assertEquals(List.of(""), invocations);
    }

    @Test
    void changeTracking_multipleWritesInTransaction_effectRunOnce() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        Signal.runInTransaction(() -> {
            signal.set("first");
            signal.set("second");
        });

        assertEquals(List.of("", "second"), invocations);
    }

    @Test
    void changeTracking_multipleSignalsInTransaction_effectRunOnce() {
        SharedValueSignal<String> signal1 = new SharedValueSignal<>("");
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal1.value() + signal2.value());
        });

        Signal.runInTransaction(() -> {
            signal1.set("one ");
            signal2.set("two");
        });

        assertEquals(List.of("", "one two"), invocations);
    }

    @Test
    void changeTracking_changeOtherPartOfNode_effectNotRunAgain() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");
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
        AsyncSharedValueSignal signal = new AsyncSharedValueSignal();
        signal.set("");
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
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(List.of("value"), invocations);

        signal.set("value");
        assertEquals(List.of("value"), invocations);

        signal.set("update");
        assertEquals(List.of("value", "update"), invocations);
    }

    @Test
    void changeTracking_readChildNodes_coveredByNextEffectInvocation() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
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
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        assertEquals(Arrays.asList("initial"), invocations);

        signal.set(null);
        assertEquals(Arrays.asList("initial", null), invocations);
    }

    @Test
    void changeTracking_lambdaSignal_changeTracked() {
        SharedValueSignal<Integer> signal = new SharedValueSignal<>(1);
        Signal<Integer> doubled = () -> signal.value() * 2;

        ArrayList<Integer> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(doubled.value());
        });

        assertEquals(List.of(2), invocations);

        signal.set(2);
        assertEquals(List.of(2, 4), invocations);
    }

    @Test
    void close_effectReadsValue_affectNotRunAfterClose() {
        ArrayList<String> invocations = new ArrayList<>();
        SharedValueSignal<String> signal = new SharedValueSignal<>("");

        CleanupCallback closer = Signal.effect(() -> {
            invocations.add(signal.value());
        });

        closer.cleanup();
        signal.set("update");

        assertEquals(List.of(""), invocations);
    }

    @Test
    void dispatcher_multipleWrites_singleUpdateWhenDispatcherTriggers() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        TestExecutor dispatcher = useTestEffectDispatcher();

        ArrayList<String> invocations = new ArrayList<>();

        Signal.effect(() -> {
            invocations.add(signal.value());
        });
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);

        signal.set("update1");
        signal.set("update2");
        assertEquals(List.of("initial"), invocations);

        dispatcher.runPendingTasks();
        assertEquals(List.of("initial", "update2"), invocations);
    }

    @Test
    void dispatcher_closeWithPendingUpdate_noUpdate() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        TestExecutor dispatcher = useTestEffectDispatcher();

        ArrayList<String> invocations = new ArrayList<>();

        CleanupCallback closer = Signal.effect(() -> {
            invocations.add(signal.value());
        });
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);

        signal.set("update");
        assertEquals(List.of("initial"), invocations);

        closer.cleanup();
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsException_effectRemainsFunctional() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        RuntimeException exception = new RuntimeException("Expected exception");

        ArrayList<String> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
            throw exception;
        });
        assertUncaughtException(exception);

        signal.set("update");

        assertUncaughtException(exception);
        assertEquals(List.of("initial", "update"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsException_otherEffectsWork() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        RuntimeException exception = new RuntimeException("Expected exception");
        Signal.effect(() -> {
            throw exception;
        });

        assertUncaughtException(exception);

        ArrayList<String> invocations = new ArrayList<>();
        Signal.effect(() -> {
            invocations.add(signal.value());
        });

        signal.set("update");
        assertEquals(List.of("initial", "update"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsError_effectClosed() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        ArrayList<String> invocations = new ArrayList<>();
        Error error = new Error("Expected error");
        Signal.effect(() -> {
            invocations.add(signal.value());

            throw error;
        });
        assertEquals(List.of("initial"), invocations);
        assertUncaughtException(caught -> caught.getCause() == error);

        signal.set("update");
        assertEquals(List.of("initial"), invocations);
    }

    @Test
    void infiniteLoopDetection_writeUnrelatedSignal_noError() {
        SharedValueSignal<String> other = new SharedValueSignal<>("other");
        SharedValueSignal<String> signal = new SharedValueSignal<>("signal");

        Signal.effect(() -> {
            other.set(signal.value());
        });
        assertEquals("signal", other.value());

        signal.set("update");
        assertEquals("update", other.value());
    }

    @Test
    void infiniteLoopDetection_writeOwnSignal_loopDetected() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("signal");
        SharedValueSignal<String> trigger = new SharedValueSignal<>("trigger");

        AtomicInteger count = new AtomicInteger();

        Signal.effect(() -> {
            count.incrementAndGet();
            trigger.value();
            signal.value();

            assertThrows(IllegalStateException.class, () -> {
                signal.set("update");
            });
        });

        assertEquals(1, count.get());

        trigger.set("update");

        assertEquals(1, count.get(), "Signal should have been disabled");
    }

    @Test
    void infiniteLoopDetection_loopBetweenEffects_loopDetectedFromSetter() {
        SharedValueSignal<String> signal1 = new SharedValueSignal<>("signal");
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("signal");

        AtomicInteger throwCount = new AtomicInteger();

        Signal.effect(() -> {
            String value = signal2.value() + " update";

            try {
                signal1.set(value);
            } catch (IllegalStateException e) {
                throwCount.incrementAndGet();
            }
        });
        assertEquals(0, throwCount.get(),
                "Should not fail with only one effect active");

        Signal.effect(() -> {
            signal2.set(signal1.value() + " update");
        });
        assertEquals(1, throwCount.get(),
                "Should fail when the other effect is created");
    }

    @Test
    void infiniteLoopDetection_loopBetweenConditionalEffects_loopDetected() {
        SharedValueSignal<String> signal1 = new SharedValueSignal<>("signal");
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("signal");
        SharedValueSignal<Boolean> trigger = new SharedValueSignal<>(false);

        Signal.effect(() -> {
            signal1.set(signal2.value() + " update");
        });

        Signal.effect(() -> {
            if (trigger.value()) {
                signal2.set(signal1.value() + " update");
            }
        });
        assertNoUncaughtException();

        trigger.set(true);
        assertUncaughtException(IllegalStateException.class);
    }

    @Test
    void infiniteLoopDetection_loopBetweenAsyncEffects_loopDetected() {
        TestExecutor dispatcher = useTestEffectDispatcher();

        SharedValueSignal<String> signal1 = new SharedValueSignal<>("signal");
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("signal");

        Signal.effect(() -> {
            signal1.set(signal2.value() + " update");
        });
        dispatcher.runPendingTasks();

        Signal.effect(() -> {
            signal2.set(signal1.value() + " update");
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

        SharedValueSignal<String> signal = new SharedValueSignal<>("signal") {
            @Override
            protected Usage createUsage(Transaction transaction) {
                Usage usage = super.createUsage(transaction);

                return new Usage() {
                    @Override
                    public boolean hasChanges() {
                        return usage.hasChanges();
                    }

                    @Override
                    public CleanupCallback onNextChange(
                            TransientListener listener) {
                        /*
                         * Emulate race condition by injecting an unrelated
                         * write at the moment when the effect starts listening
                         * for changes
                         */
                        set("update");

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
