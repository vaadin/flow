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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.MissingSignalUsageException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;
import com.vaadin.flow.signals.shared.SharedValueSignalTest.AsyncSharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class EffectTest extends SignalTestBase {

    @Test
    void newEffect_noSignalUsage_throws() {
        assertThrows(MissingSignalUsageException.class, () -> {
            Signal.unboundEffect(() -> {
            });
        });
    }

    @Test
    void newEffect_actionIsRunOnce() {
        var dependency = createDependency();
        AtomicInteger count = new AtomicInteger();

        Signal.unboundEffect(() -> {
            dependency.get();
            count.incrementAndGet();
        });

        assertEquals(1, count.get());
    }

    @Test
    void newEffect_closeImmediately_actionIsRunOnce() {
        var dependency = createDependency();
        AtomicInteger count = new AtomicInteger();

        Signal.unboundEffect(() -> {
            dependency.get();
            count.incrementAndGet();
        }).remove();

        assertEquals(1, count.get());
    }

    @Test
    void changeTracking_effectReadsValue_effectRunAgain() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
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

        Signal.unboundEffect(() -> {
            invocations.add(signal.get().size());
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

        Signal.unboundEffect(() -> {
            invocations.add(signal.get().size());
        });

        assertEquals(List.of(0), invocations);

        signal.put("key", "value");
        assertEquals(List.of(0, 1), invocations);

        signal.remove("key");
        assertEquals(List.of(0, 1, 0), invocations);
    }

    @Test
    void changeTracking_effectStopsReadingValue_effectNotRunAgain() {
        var dependency = createDependency();
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();
        AtomicBoolean read = new AtomicBoolean(true);

        Signal.unboundEffect(() -> {
            dependency.get();
            if (read.get()) {
                invocations.add(signal.get());
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
        var dependency = createDependency();
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();
        AtomicBoolean read = new AtomicBoolean(true);

        Signal.unboundEffect(() -> {
            dependency.get();
            if (read.get()) {
                invocations.add(signal.get());
            } else {
                invocations.add(Signal.untracked(() -> {
                    return "untracked: " + signal.get();
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

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        signal.replace("foo", "bar");
        assertEquals(List.of(""), invocations);
    }

    @Test
    void changeTracking_multipleWritesInTransaction_effectRunOnce() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        Signal.runInTransaction(() -> {
            signal.set("first");
            signal.set("second");
        });

        assertEquals(List.of("", "second"), invocations);
    }

    @Test
    void changeTracking_changeOtherPartOfNode_effectNotRunAgain() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
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

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
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

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
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

        Signal.unboundEffect(() -> {
            List<String> values = signal.get().stream().map(Signal::get)
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
    @SuppressWarnings("NullAway") // Deliberately testing null value behavior
    void changeTracking_changeValueToNull_effectTriggered() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        assertEquals(Arrays.asList("initial"), invocations);

        signal.set(null);
        assertEquals(Arrays.asList("initial", null), invocations);
    }

    @Test
    void changeTracking_lambdaSignal_changeTracked() {
        SharedValueSignal<Integer> signal = new SharedValueSignal<>(1);
        Signal<Integer> doubled = () -> signal.get() * 2;

        ArrayList<Integer> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(doubled.get());
        });

        assertEquals(List.of(2), invocations);

        signal.set(2);
        assertEquals(List.of(2, 4), invocations);
    }

    @Test
    void close_effectReadsValue_affectNotRunAfterClose() {
        ArrayList<String> invocations = new ArrayList<>();
        SharedValueSignal<String> signal = new SharedValueSignal<>("");

        Registration closer = Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        closer.remove();
        signal.set("update");

        assertEquals(List.of(""), invocations);
    }

    @Test
    void dispatcher_multipleWrites_singleUpdateWhenDispatcherTriggers() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        TestExecutor dispatcher = useTestEffectDispatcher();

        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
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

        Registration closer = Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);

        signal.set("update");
        assertEquals(List.of("initial"), invocations);

        closer.remove();
        dispatcher.runPendingTasks();
        assertEquals(List.of("initial"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsException_effectRemainsFunctional() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        RuntimeException exception = new RuntimeException("Expected exception");

        ArrayList<String> invocations = new ArrayList<>();
        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
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

        var dependency = createDependency();
        RuntimeException exception = new RuntimeException("Expected exception");
        Signal.unboundEffect(() -> {
            dependency.get();
            throw exception;
        });

        assertUncaughtException(exception);

        ArrayList<String> invocations = new ArrayList<>();
        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        signal.set("update");
        assertEquals(List.of("initial", "update"), invocations);
    }

    @Test
    void exceptionHandling_effectThrowsError_effectClosed() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        ArrayList<String> invocations = new ArrayList<>();
        Error error = new Error("Expected error");
        Signal.unboundEffect(() -> {
            invocations.add(signal.get());

            throw error;
        });
        assertEquals(List.of("initial"), invocations);
        assertUncaughtException(caught -> caught.getCause() == error);

        signal.set("update");
        assertEquals(List.of("initial"), invocations);
    }

    @Test
    void infiniteLoopDetection_cachedSignalSelfUpdate_notDetectedAsLoop() {
        SharedValueSignal<String> source = new SharedValueSignal<>("initial");
        AtomicInteger effectCount = new AtomicInteger();

        Signal<String> cached = Signal.cached(() -> source.get());

        Signal.unboundEffect(() -> {
            int count = effectCount.incrementAndGet();
            // Read cached so the effect registers a listener on cached's tree.
            cached.get();
            // Trigger a source change from inside the effect on the first run.
            // The cascade source change → cached's source-dep listener →
            // revalidateAndListen → submit cached then fires the effect's
            // listener on cached while this effect is still in activeEffects.
            // Without the fix, this is mistakenly detected as an infinite loop
            // even though the submit is just lazy re-evaluation of the cached
            // state.
            if (count == 1) {
                source.set("update");
            }
        });

        assertEquals(2, effectCount.get());
    }

    @Test
    void runInReadTriggeredUpdateContext_loopInsideContext_stillDetected() {
        // The context only hides effects that were already active outside the
        // block. Effects that activate inside the block still register
        // themselves and are therefore still subject to loop detection.
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        AtomicInteger throwCount = new AtomicInteger();

        Effect.runInReadTriggeredUpdateContext(() -> {
            Signal.unboundEffect(() -> {
                signal.get();
                try {
                    signal.set("new");
                } catch (IllegalStateException e) {
                    throwCount.incrementAndGet();
                }
            });
        });

        assertEquals(1, throwCount.get());
    }

    @Test
    void infiniteLoopDetection_writeUnrelatedSignal_noError() {
        SharedValueSignal<String> other = new SharedValueSignal<>("other");
        SharedValueSignal<String> signal = new SharedValueSignal<>("signal");

        Signal.unboundEffect(() -> {
            other.set(signal.get());
        });
        assertEquals("signal", other.peek());

        signal.set("update");
        assertEquals("update", other.peek());
    }

    @Test
    void infiniteLoopDetection_writeOwnSignal_loopDetected() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("signal");
        SharedValueSignal<String> trigger = new SharedValueSignal<>("trigger");

        AtomicInteger count = new AtomicInteger();

        Signal.unboundEffect(() -> {
            count.incrementAndGet();
            trigger.get();
            signal.get();

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

        Signal.unboundEffect(() -> {
            String value = signal2.get() + " update";

            try {
                signal1.set(value);
            } catch (IllegalStateException e) {
                throwCount.incrementAndGet();
            }
        });
        assertEquals(0, throwCount.get(),
                "Should not fail with only one effect active");

        Signal.unboundEffect(() -> {
            signal2.set(signal1.get() + " update");
        });
        assertEquals(1, throwCount.get(),
                "Should fail when the other effect is created");
    }

    @Test
    void infiniteLoopDetection_loopBetweenConditionalEffects_loopDetected() {
        SharedValueSignal<String> signal1 = new SharedValueSignal<>("signal");
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("signal");
        SharedValueSignal<Boolean> trigger = new SharedValueSignal<>(false);

        Signal.unboundEffect(() -> {
            signal1.set(signal2.get() + " update");
        });

        Signal.unboundEffect(() -> {
            if (trigger.get()) {
                signal2.set(signal1.get() + " update");
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

        Signal.unboundEffect(() -> {
            signal1.set(signal2.get() + " update");
        });
        dispatcher.runPendingTasks();

        Signal.unboundEffect(() -> {
            signal2.set(signal1.get() + " update");
        });
        // Runs the 2nd effect which schedules running the 1st effect
        dispatcher.runPendingTasks();
        assertNoUncaughtException();

        // Runs the 1st effect which is where the loop can be detected
        dispatcher.runPendingTasks();
        assertUncaughtException(IllegalStateException.class);
    }

    @Test
    void setDispatcher_changesDispatcherUsedForSubsequentRuns() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        ArrayList<String> dispatcherLog = new ArrayList<>();
        ArrayList<String> invocations = new ArrayList<>();

        Effect effect = new Effect(() -> {
            invocations.add(signal.get());
        }, Runnable::run);
        assertEquals(List.of("initial"), invocations);

        effect.passivate();

        // Switch to a dispatcher that records calls
        effect.setDispatcher(command -> {
            dispatcherLog.add("dispatched");
            command.run();
        });

        signal.set("updated");
        effect.activate();
        // activate() calls revalidate() directly (not via the dispatcher)
        // when there are changes, so the dispatcher is not invoked here
        assertEquals(List.of("initial", "updated"), invocations,
                "Effect should re-run with new value after setDispatcher + activate");
        assertEquals(0, dispatcherLog.size(),
                "Dispatcher is not used by activate() directly");

        // The new dispatcher IS used for subsequent signal change notifications
        signal.set("final");
        assertEquals(List.of("initial", "updated", "final"), invocations,
                "Effect should continue using the new dispatcher");
        assertEquals(1, dispatcherLog.size(),
                "New dispatcher should have been used for signal change");
    }

    @Test
    void passivateActivate_noChanges_callbackNotReRun() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        ArrayList<String> invocations = new ArrayList<>();

        Effect effect = new Effect(() -> {
            invocations.add(signal.get());
        });
        assertEquals(List.of("initial"), invocations);

        effect.passivate();
        signal.set("initial"); // no-op change
        effect.activate();
        assertEquals(List.of("initial"), invocations,
                "Callback should not re-run when nothing changed");

        signal.set("update");
        assertEquals(List.of("initial", "update"), invocations,
                "Effect should remain active after activate");
    }

    @Test
    void passivateActivate_withChanges_callbackReRun() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        ArrayList<String> invocations = new ArrayList<>();

        Effect effect = new Effect(() -> {
            invocations.add(signal.get());
        });
        assertEquals(List.of("initial"), invocations);

        effect.passivate();
        signal.set("changed");
        effect.activate();
        assertEquals(List.of("initial", "changed"), invocations,
                "Callback should re-run when dependency changed");
    }

    @Test
    void passivateActivate_noChanges_nextChangeIsNotInitialRun() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        List<Boolean> initialRuns = new ArrayList<>();

        Effect effect = new Effect(ctx -> {
            signal.get();
            initialRuns.add(ctx.isInitialRun());
        });
        assertEquals(List.of(true), initialRuns);

        effect.passivate();
        effect.activate();

        signal.set("update");
        assertEquals(List.of(true, false), initialRuns,
                "Change after activate without changes should not be initial run");
    }

    @Test
    void passivateActivate_racyChangeDuringReRegister_isInitialRunTrue() {
        AtomicBoolean injectChange = new AtomicBoolean(false);

        SharedValueSignal<String> signal = new SharedValueSignal<>("initial") {
            @Override
            protected Usage createUsage(Transaction transaction) {
                Usage usage = super.createUsage(transaction);

                return new Usage() {
                    @Override
                    public boolean hasChanges() {
                        return usage.hasChanges();
                    }

                    @Override
                    public Registration onNextChange(
                            TransientListener listener) {
                        if (injectChange.compareAndSet(true, false)) {
                            set("sneaky");
                        }
                        return usage.onNextChange(listener);
                    }
                };
            }
        };

        List<Boolean> initialRuns = new ArrayList<>();

        Effect effect = new Effect(ctx -> {
            signal.get();
            initialRuns.add(ctx.isInitialRun());
        });
        assertEquals(List.of(true), initialRuns);

        effect.passivate();
        injectChange.set(true);
        effect.activate();

        assertEquals(List.of(true, true), initialRuns,
                "Change during activation should run with isInitialRun=true");
    }

    @Test
    void passivateActivate_asyncDispatcher_racyChange_isInitialRunTrue() {
        TestExecutor dispatcher = useTestEffectDispatcher();
        AtomicBoolean injectChange = new AtomicBoolean(false);

        SharedValueSignal<String> signal = new SharedValueSignal<>("initial") {
            @Override
            protected Usage createUsage(Transaction transaction) {
                Usage usage = super.createUsage(transaction);

                return new Usage() {
                    @Override
                    public boolean hasChanges() {
                        return usage.hasChanges();
                    }

                    @Override
                    public Registration onNextChange(
                            TransientListener listener) {
                        if (injectChange.compareAndSet(true, false)) {
                            set("sneaky");
                        }
                        return usage.onNextChange(listener);
                    }
                };
            }
        };

        List<Boolean> initialRuns = new ArrayList<>();

        Effect effect = new Effect(ctx -> {
            signal.get();
            initialRuns.add(ctx.isInitialRun());
        });
        dispatcher.runPendingTasks();
        assertEquals(List.of(true), initialRuns);

        effect.passivate();
        injectChange.set(true);
        effect.activate();
        // Revalidation is dispatched asynchronously; firstRun must not
        // be reset before the dispatcher runs.
        dispatcher.runPendingTasks();

        assertEquals(List.of(true, true), initialRuns,
                "Racy change with async dispatcher should run with isInitialRun=true");
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
                    public Registration onNextChange(
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

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
        });

        dispatcher.runPendingTasks();
        assertEquals(List.of("signal"), invocations);

        dispatcher.runPendingTasks();
        assertEquals(List.of("signal", "update"), invocations);
    }

    @Test
    void ownerUI_sameUICurrent_notBackgroundChange() {
        UI ui = Mockito.mock(UI.class);
        UI.setCurrent(ui);

        ValueSignal<String> signal = new ValueSignal<>("hello");
        List<Boolean> backgroundChanges = new ArrayList<>();

        Effect effect = new Effect(ctx -> {
            signal.get();
            backgroundChanges.add(ctx.isBackgroundChange());
        }, Runnable::run);
        effect.setOwnerUI(ui);

        assertEquals(1, backgroundChanges.size());
        assertFalse(backgroundChanges.get(0));

        signal.set("update");

        assertEquals(2, backgroundChanges.size());
        assertFalse(backgroundChanges.get(1),
                "Change with same UI should not be background");
    }

    @Test
    void ownerUI_noUICurrent_isBackgroundChange() {
        UI ui = Mockito.mock(UI.class);
        UI.setCurrent(ui);

        ValueSignal<String> signal = new ValueSignal<>("hello");
        List<Boolean> backgroundChanges = new ArrayList<>();

        Effect effect = new Effect(ctx -> {
            signal.get();
            backgroundChanges.add(ctx.isBackgroundChange());
        }, Runnable::run);
        effect.setOwnerUI(ui);

        assertEquals(1, backgroundChanges.size());

        UI.setCurrent(null);
        signal.set("from background");

        assertEquals(2, backgroundChanges.size());
        assertTrue(backgroundChanges.get(1),
                "Change without UI should be background");
    }

    @Test
    void ownerUI_differentUICurrent_isBackgroundChange() {
        UI uiA = Mockito.mock(UI.class);
        UI uiB = Mockito.mock(UI.class);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        List<Boolean> backgroundChangesA = new ArrayList<>();
        List<Boolean> backgroundChangesB = new ArrayList<>();

        UI.setCurrent(uiA);
        Effect effectA = new Effect(ctx -> {
            signal.get();
            backgroundChangesA.add(ctx.isBackgroundChange());
        }, Runnable::run);
        effectA.setOwnerUI(uiA);

        UI.setCurrent(uiB);
        Effect effectB = new Effect(ctx -> {
            signal.get();
            backgroundChangesB.add(ctx.isBackgroundChange());
        }, Runnable::run);
        effectB.setOwnerUI(uiB);

        assertEquals(1, backgroundChangesA.size());
        assertEquals(1, backgroundChangesB.size());

        // User A modifies the signal
        UI.setCurrent(uiA);
        signal.set("from user A");

        assertEquals(2, backgroundChangesA.size());
        assertFalse(backgroundChangesA.get(1),
                "Change from own UI should not be background");

        assertEquals(2, backgroundChangesB.size());
        assertTrue(backgroundChangesB.get(1),
                "Change from another UI should be background");
    }

    @Test
    void noOwnerUI_fallsBackToVaadinRequestCheck() {
        VaadinRequest mockRequest = Mockito.mock(VaadinRequest.class);
        CurrentInstance.set(VaadinRequest.class, mockRequest);

        ValueSignal<String> signal = new ValueSignal<>("hello");
        List<Boolean> backgroundChanges = new ArrayList<>();

        // Effect without ownerUI (like Signal.unboundEffect)
        new Effect(ctx -> {
            signal.get();
            backgroundChanges.add(ctx.isBackgroundChange());
        }, Runnable::run);

        assertEquals(1, backgroundChanges.size());
        assertFalse(backgroundChanges.get(0));

        // Change with VaadinRequest present — not background
        signal.set("with request");

        assertEquals(2, backgroundChanges.size());
        assertFalse(backgroundChanges.get(1),
                "Change with VaadinRequest should not be background");

        // Clear VaadinRequest — background
        CurrentInstance.set(VaadinRequest.class, null);
        signal.set("without request");

        assertEquals(3, backgroundChanges.size());
        assertTrue(backgroundChanges.get(2),
                "Change without VaadinRequest should be background");
    }

    @Test
    void changeTracking_readSameSignalMultipleTimes_effectRunOnlyOnce() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal.get());
            signal.get();
            signal.get();
        });

        assertEquals(List.of("initial"), invocations);

        signal.set("update");
        assertEquals(List.of("initial", "update"), invocations);

        signal.set("again");
        assertEquals(List.of("initial", "update", "again"), invocations);
    }

    @Test
    void changeTracking_readMultipleDifferentSignals_effectRunsOncePerChange() {
        SharedValueSignal<String> signal1 = new SharedValueSignal<>("a");
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("b");
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            invocations.add(signal1.get() + signal2.get());
        });

        assertEquals(List.of("ab"), invocations);

        signal1.set("A");
        assertEquals(List.of("ab", "Ab"), invocations);

        signal2.set("B");
        assertEquals(List.of("ab", "Ab", "AB"), invocations);
    }

    @Test
    void changeTracking_multipleReadsFromTwoSignals_eachSignalChangeTriggersEffectOnce() {
        SharedValueSignal<String> signal1 = new SharedValueSignal<>("a");
        SharedValueSignal<Integer> signal2 = new SharedValueSignal<>(1);
        AtomicInteger effectRunCount = new AtomicInteger(0);
        ArrayList<String> invocations = new ArrayList<>();

        Signal.unboundEffect(() -> {
            effectRunCount.incrementAndGet();
            // Read signal1 three times
            String s1 = signal1.get();
            signal1.get();
            signal1.get();
            // Read signal2 two times
            Integer s2 = signal2.get();
            signal2.get();
            invocations.add(s1 + ":" + s2);
        });

        assertEquals(1, effectRunCount.get(),
                "Effect should run exactly once initially");
        assertEquals(List.of("a:1"), invocations);

        signal1.set("b");
        assertEquals(2, effectRunCount.get(),
                "Effect should run exactly once when signal1 changes (despite 3 reads)");
        assertEquals(List.of("a:1", "b:1"), invocations);

        signal2.set(2);
        assertEquals(3, effectRunCount.get(),
                "Effect should run exactly once when signal2 changes (despite 2 reads)");
        assertEquals(List.of("a:1", "b:1", "b:2"), invocations);

        signal1.set("c");
        assertEquals(4, effectRunCount.get(),
                "Effect should run exactly once when signal1 changes again");
        assertEquals(List.of("a:1", "b:1", "b:2", "c:2"), invocations);

        signal2.set(3);
        assertEquals(5, effectRunCount.get(),
                "Effect should run exactly once when signal2 changes again");
        assertEquals(List.of("a:1", "b:1", "b:2", "c:2", "c:3"), invocations);
    }

    @Test
    void initialRun_concurrentSignalWrite_doesNotDeadlock() throws Exception {
        // ABBA deadlock regression. Effect.<init> runs revalidate()
        // (which calls the action that reads signals, acquiring the
        // SignalTree lock). It shouldn't run inside a synchronized(this) block
        // because a concurrent
        // signal write on another thread acquires the SignalTree lock first
        // and then needs the same Effect monitor (because scheduleInvalidate
        // dispatches inline when the writer thread has no environment
        // dispatcher). The opposite acquire orders form a deadlock.
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        CountDownLatch firstReadDone = new CountDownLatch(1);
        AtomicReference<Throwable> threadBFailure = new AtomicReference<>();

        Thread writer = new Thread(() -> {
            try {
                firstReadDone.await();
                signal.set("update");
            } catch (Throwable t) {
                threadBFailure.set(t);
            }
        }, "deadlock-test-writer");
        writer.setDaemon(true);

        AtomicReference<Throwable> threadAFailure = new AtomicReference<>();
        AtomicBoolean effectCreated = new AtomicBoolean();
        // Once the deadlock is fixed, the writer's notifyObservers fires
        // the just-installed observer synchronously and the effect action
        // re-runs on the writer thread. Guard side effects so re-entry is
        // safe.
        AtomicBoolean racePrimed = new AtomicBoolean();

        Thread reader = new Thread(() -> {
            try {
                new Effect(() -> {
                    // First read registers an observer on the signal.
                    signal.get();
                    if (racePrimed.compareAndSet(false, true)) {
                        // Let the writer start its set() call.
                        firstReadDone.countDown();
                        writer.start();
                        // Wait until the writer is BLOCKED entering
                        // Effect.invalidate (deadlock scenario) or has
                        // already finished (fixed scenario). Either way
                        // the next read happens after the writer has had
                        // a chance to drive its side of the race.
                        long deadline = System.currentTimeMillis() + 2000;
                        while (System.currentTimeMillis() < deadline) {
                            Thread.State state = writer.getState();
                            if (state == Thread.State.BLOCKED
                                    || state == Thread.State.TERMINATED) {
                                break;
                            }
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    signal.get();
                }, Runnable::run);
                effectCreated.set(true);
            } catch (Throwable t) {
                threadAFailure.set(t);
            }
        }, "deadlock-test-reader");
        reader.setDaemon(true);

        reader.start();
        reader.join(5000);
        writer.join(1000);

        if (reader.isAlive() || writer.isAlive()) {
            ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
            long[] deadlocked = tmx.findDeadlockedThreads();
            fail("Effect creation deadlocked with a concurrent signal write."
                    + " reader alive=" + reader.isAlive() + ", writer alive="
                    + writer.isAlive()
                    + (deadlocked != null
                            ? ", deadlocked thread IDs="
                                    + Arrays.toString(deadlocked)
                            : ""));
        }

        if (threadAFailure.get() != null) {
            throw new AssertionError("Reader thread failed",
                    threadAFailure.get());
        }
        if (threadBFailure.get() != null) {
            throw new AssertionError("Writer thread failed",
                    threadBFailure.get());
        }
        assertTrue(effectCreated.get(),
                "Effect should have been created without deadlock");
    }

    @Test
    void dispose_concurrentSignalWrite_doesNotDeadlock() throws Exception {
        // ABBA deadlock probe for dispose(). dispose() is synchronized
        // on the Effect monitor and calls clearRegistrations(), whose
        // Registration::remove acquires the SignalTree lock. Concurrently,
        // a signal write holds the SignalTree lock while notifyObservers
        // invokes the listener that schedules invalidate(), which now
        // takes the Effect monitor briefly. Opposite acquire orders form
        // a deadlock.
        runEffectVsSignalWriteDeadlockProbe("dispose", false, Effect::dispose);
    }

    @Test
    void passivate_concurrentSignalWrite_doesNotDeadlock() throws Exception {
        // Same ABBA shape as dispose(): passivate() is synchronized and
        // calls clearRegistrations() (Registration::remove acquires the
        // SignalTree lock) while a concurrent signal writer holds the
        // SignalTree lock and reaches Effect.invalidate, which needs the
        // Effect monitor.
        runEffectVsSignalWriteDeadlockProbe("passivate", false,
                Effect::passivate);
    }

    @Test
    void activate_concurrentSignalWrite_doesNotDeadlock() throws Exception {
        // activate()'s fast-path re-registers observers via
        // usage.onNextChange() under the Effect monitor. onNextChange
        // acquires the SignalTree lock, while a concurrent writer holding
        // that lock invokes the just-registered observer, drives
        // scheduleInvalidate, and Effect.invalidate then needs the Effect
        // monitor. Opposite acquire orders form a deadlock.
        runEffectVsSignalWriteDeadlockProbe("activate", true, effect -> {
            effect.passivate();
            effect.activate();
        });
    }

    private static void runEffectVsSignalWriteDeadlockProbe(String name,
            boolean delayWriter, Consumer<Effect> lifecycleAction)
            throws Exception {
        // The race is timing-sensitive, so try many iterations. The first
        // deadlock trips the ThreadMXBean check and fails the test.
        int iterations = 200;
        long perIterationTimeoutMs = 2000;

        for (int i = 0; i < iterations; i++) {
            SharedValueSignal<String> signal = new SharedValueSignal<>("v" + i);

            // Read the signal multiple times so the action registers
            // multiple usages on the same signal. That widens the window
            // for the lifecycle action's per-registration lock acquisitions
            // to race the writer.
            Effect effect = new Effect(() -> {
                for (int k = 0; k < 8; k++) {
                    signal.get();
                }
            }, Runnable::run);

            CountDownLatch ready = new CountDownLatch(1);
            AtomicReference<Throwable> writerFailure = new AtomicReference<>();
            AtomicReference<Throwable> callerFailure = new AtomicReference<>();

            Thread writer = new Thread(() -> {
                try {
                    ready.await();
                    signal.set("update-" + System.nanoTime());
                } catch (Throwable t) {
                    writerFailure.set(t);
                }
            }, name + "-writer-" + i);
            writer.setDaemon(true);

            Thread caller = new Thread(() -> {
                try {
                    ready.await();
                    lifecycleAction.accept(effect);
                } catch (Throwable t) {
                    callerFailure.set(t);
                }
            }, name + "-caller-" + i);
            caller.setDaemon(true);

            writer.start();
            caller.start();
            // For lifecycle actions that need to be entered before the
            // writer drives notifyObservers (activate's fast-path needs
            // the observers to be installed mid-loop), nudge the caller
            // ahead with a small head start.
            if (delayWriter) {
                Thread.yield();
            }
            ready.countDown();

            writer.join(perIterationTimeoutMs);
            caller.join(perIterationTimeoutMs);

            if (writer.isAlive() || caller.isAlive()) {
                ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
                long[] deadlocked = tmx.findDeadlockedThreads();
                StringBuilder sb = new StringBuilder(name)
                        .append(" deadlocked on iteration ").append(i)
                        .append(". writer alive=").append(writer.isAlive())
                        .append(", caller alive=").append(caller.isAlive());
                if (deadlocked != null) {
                    sb.append(", deadlocked thread IDs=")
                            .append(Arrays.toString(deadlocked));
                    for (var info : tmx.getThreadInfo(deadlocked, true, true)) {
                        sb.append("\n  ").append(info.getThreadName())
                                .append(" waiting on ")
                                .append(info.getLockName()).append(" owned by ")
                                .append(info.getLockOwnerName());
                        for (var frame : info.getStackTrace()) {
                            sb.append("\n    at ").append(frame);
                        }
                    }
                }
                fail(sb.toString());
            }

            if (writerFailure.get() != null) {
                throw new AssertionError(
                        name + " writer failed on iteration " + i,
                        writerFailure.get());
            }
            if (callerFailure.get() != null) {
                throw new AssertionError(
                        name + " caller failed on iteration " + i,
                        callerFailure.get());
            }
        }
    }
}
