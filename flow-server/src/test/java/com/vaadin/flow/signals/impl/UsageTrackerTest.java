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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.impl.UsageTracker.CombinedUsage;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsageTrackerTest extends SignalTestBase {
    @Test
    void hasChanges_runInTransaction_readsFromTransaction() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    signal.get();
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        Signal.runInTransaction(() -> {
            signal.set("changed");

            assertTrue(usage.hasChanges());

            Signal.runWithoutTransaction(() -> {
                assertFalse(usage.hasChanges());
            });
        });
    }

    @Test
    void track_noUsage_noChanges() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        assertFalse(usage.hasChanges());
    }

    @Test
    void track_readValueInCallback_tracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    signal.get();
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        signal.set("update");
        assertTrue(usage.hasChanges());
    }

    @Test
    void track_peekInCallback_notTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    signal.peek();
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_peekConfirmedInCallback_notTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    signal.peekConfirmed();
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_useValue_notRegistered() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    Signal.untracked(() -> {
                        signal.get();
                        return null;
                    });
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_writeInCallback_allowedNoUsageTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    signal.set("update");
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        signal.set("another");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_multipleUsages_combinedUsage() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        assertInstanceOf(CombinedUsage.class, usage);
    }

    @Test
    void track_singleUsage_notCombinedUsage() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                });
        trackedSupplier.supply();
        Usage usage = trackedSupplier.dependencies();

        assertFalse(usage instanceof CombinedUsage);
    }

    @Test
    void isActive_activeInsideTrackerInactiveOutsdide() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    assertTrue(UsageTracker.isActive());
                    Signal.untracked(() -> {
                        assertFalse(UsageTracker.isActive());
                        return null;
                    });
                    return null;
                });
        trackedSupplier.supply();

        assertFalse(UsageTracker.isActive());
    }

    @Test
    void combinedUsage_anyUsageChanged_isChanged() {
        TestUsage a = new TestUsage();
        TestUsage b = new TestUsage();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));

        assertFalse(usage.hasChanges());

        a.hasChanges = true;
        assertTrue(usage.hasChanges());

        b.hasChanges = true;
        assertTrue(usage.hasChanges());

        a.hasChanges = false;
        assertTrue(usage.hasChanges());

        b.hasChanges = false;
        assertFalse(usage.hasChanges());
    }

    @Test
    void combinedUsage_onNextChange_registersWithAll() {
        TestUsage a = new TestUsage();
        TestUsage b = new TestUsage();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        Registration cleanup = usage.onNextChange(immediate -> false);

        assertEquals(1, a.listeners.size());
        assertEquals(1, b.listeners.size());

        cleanup.remove();
        assertEquals(0, a.listeners.size());
        assertEquals(0, b.listeners.size());
    }

    @Test
    void combinedOnNextChange_nonRepeatingListener_removedAfterFirstTrigger() {
        TestUsage a = new TestUsage();
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return false;
        });

        boolean keep = a.listeners.get(0).invoke(false);
        assertFalse(keep);
        assertEquals(1, count.intValue());

        assertEquals(0, a.listeners.size());
        assertEquals(0, b.listeners.size());
    }

    @Test
    void combinedOnNextChange_repeatingListener_remainsInUse() {
        TestUsage a = new TestUsage();
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return true;
        });

        boolean keep = a.listeners.get(0).invoke(false);
        assertTrue(keep);
        assertEquals(1, count.intValue());

        assertEquals(1, a.listeners.size());
        assertEquals(1, b.listeners.size());
    }

    @Test
    void combinedOnNextChange_immediatelyNotifiedNonRepeatingListener_immediatelyNotifiedThenRemoved() {
        TestUsage a = new TestUsage() {
            @Override
            public Registration onNextChange(TransientListener listener) {
                Registration cleanup = super.onNextChange(listener);
                listener.invoke(true);
                return cleanup;
            }
        };
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return false;
        });

        assertEquals(1, count.intValue());

        assertEquals(0, a.listeners.size());
        assertEquals(0, b.listeners.size());
    }

    @Test
    void combinedOnNextChange_immediatelyNotifiedRepeatingListener_immediatelyNotifiedAndKeptInUse() {
        TestUsage a = new TestUsage() {
            @Override
            public Registration onNextChange(TransientListener listener) {
                Registration cleanup = super.onNextChange(listener);
                listener.invoke(true);
                return cleanup;
            }
        };
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return true;
        });

        assertEquals(1, count.intValue());

        assertEquals(1, a.listeners.size());
        assertEquals(1, b.listeners.size());
    }

    @Test
    void trackedSupplier_returnsValue() {
        var testValue = new Object();

        UsageTracker.TrackedSupplier<Object> trackedSupplier = UsageTracker
                .tracked(() -> testValue);

        assertEquals(testValue, trackedSupplier.supply());
    }

    @Test
    void trackedSupplier_noUsage_returnsNoUsage() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    // No signal reads
                    return null;
                });
        trackedSupplier.supply();

        assertSame(UsageTracker.NO_USAGE, trackedSupplier.dependencies());
    }

    @Test
    void trackedSupplier_singleUsage_returnsUsage() {
        TestUsage testUsage = new TestUsage();

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(testUsage);
                    return null;
                });
        trackedSupplier.supply();

        Usage result = trackedSupplier.dependencies();
        assertEquals(result, testUsage);
    }

    @Test
    void trackedSupplier_multipleUsages_returnsCombinedUsage() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                });
        trackedSupplier.supply();

        assertInstanceOf(CombinedUsage.class, trackedSupplier.dependencies());
    }

    @Test
    void trackedSupplier_dependenciesCalledTwice_returnsSameResult() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                });
        trackedSupplier.supply();

        // Call dependencies() twice
        Usage first = trackedSupplier.dependencies();
        Usage second = trackedSupplier.dependencies();

        // Should return the same cached result
        assertSame(first, second);
    }

    @Test
    void assertHasUsage_noUsage_throwsMissingSignalUsageException() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    // No signal reads
                    return null;
                });
        trackedSupplier.supply();

        UsageTracker.MissingSignalUsageException exception = assertThrows(
                UsageTracker.MissingSignalUsageException.class,
                () -> trackedSupplier
                        .assertHasUsage("Custom context message."));
        assertTrue(exception.getMessage().contains("Custom context message."));
    }

    @Test
    void missingSignalUsageException_messageFormat() {
        UsageTracker.MissingSignalUsageException exception = new UsageTracker.MissingSignalUsageException(
                "Test reason.");

        assertTrue(exception.getMessage().contains("Test reason."));
        assertTrue(exception.getMessage()
                .contains("Expected at least one signal value read"));
    }

    @Test
    void assertHasUsage_withUsage_returnsUsage() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                });
        trackedSupplier.supply();

        assertDoesNotThrow(
                () -> trackedSupplier.assertHasUsage("Failed test."));
    }

    @Test
    void assertNoUsage_registerUsage_throwsDeniedSignalUsageException() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                });
        trackedSupplier.supply();

        UsageTracker.DeniedSignalUsageException exception = assertThrows(
                UsageTracker.DeniedSignalUsageException.class,
                () -> trackedSupplier
                        .assertNoUsage("Signal access not allowed here."));
        assertTrue(exception.getMessage()
                .contains("Signal access not allowed here."));
    }

    @Test
    void assertNoUsage_neverRegistered_returnsNoUsage() {
        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .tracked(() -> {
                    // Never call register
                    return null;
                });
        trackedSupplier.supply();

        assertDoesNotThrow(() -> trackedSupplier.assertNoUsage("Failed test."));
    }

    @Test
    void deniedSignalUsageException_messageFormat() {
        UsageTracker.DeniedSignalUsageException exception = new UsageTracker.DeniedSignalUsageException(
                "Custom context.");

        assertTrue(exception.getMessage().contains("Custom context."));
        assertTrue(exception.getMessage()
                .contains("Using signals is denied in this context"));
    }

    @Test
    void trackedSupplier_withUsageListener_invokesListener() {
        class TestUsageException extends RuntimeException {
        }
        UsageTracker.UsageRegistrar preventPrematureChangeListener = new UsageTracker.UsageRegistrar() {
            @Override
            public void register(Usage usage) {
                throw new TestUsageException();
            }
        };

        UsageTracker.TrackedSupplier<Void> trackedSupplier = UsageTracker
                .<Void> tracked(() -> {
                    UsageTracker.registerUsage(new TestUsage());
                    return null;
                }).withUsageListener(preventPrematureChangeListener);

        assertThrows(TestUsageException.class, trackedSupplier::supply);
    }

    private static class TestUsage implements Usage {
        boolean hasChanges;
        List<TransientListener> listeners = new ArrayList<>();

        @Override
        public boolean hasChanges() {
            return hasChanges;
        }

        @Override
        @NonNull
        public Registration onNextChange(@NonNull TransientListener listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }
    }
}
