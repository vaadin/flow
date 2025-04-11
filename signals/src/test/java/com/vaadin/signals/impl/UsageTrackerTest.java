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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.ValueSignal;
import com.vaadin.signals.impl.UsageTracker.CombinedUsage;
import com.vaadin.signals.impl.UsageTracker.Usage;

public class UsageTrackerTest extends SignalTestBase {
    @Test
    void hasChanges_runInTransaction_readsFromTransaction() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        Signal.runInTransaction(() -> {
            signal.value("changed");

            assertTrue(usage.hasChanges());

            Signal.runWithoutTransaction(() -> {
                assertFalse(usage.hasChanges());
            });
        });
    }

    @Test
    void track_noUsage_noChnages() {
        Usage usage = UsageTracker.track(() -> {
        });

        assertFalse(usage.hasChanges());
    }

    @Test
    void track_readValueInCallback_tracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.value("update");
        assertTrue(usage.hasChanges());
    }

    @Test
    void track_peekInCallback_notTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.peek();
        });

        signal.value("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_peekConfirmedInCallback_notTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.peekConfirmed();
        });

        signal.value("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_writeInCallback_notAllowedNoUsageTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            assertThrows(IllegalStateException.class, () -> {
                signal.value("update");
            });
        });

        signal.value("another");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_useValue_notRegistered() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            Signal.untracked(() -> {
                signal.value();
                return null;
            });
        });

        signal.value("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_writeInCallback_allowedNoUsageTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            Signal.untracked(() -> {
                signal.value("update");
                return null;
            });
        });

        signal.value("another");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_multipleUsages_combinedUsage() {
        Usage usage = UsageTracker.track(() -> {
            UsageTracker.registerUsage(new TestUsage());
            UsageTracker.registerUsage(new TestUsage());
        });

        assertInstanceOf(CombinedUsage.class, usage);
    }

    @Test
    void track_singleUsage_notCombinedUsage() {
        Usage usage = UsageTracker.track(() -> {
            UsageTracker.registerUsage(new TestUsage());
        });

        assertFalse(usage instanceof CombinedUsage);
    }

    @Test
    void isActive_activeInsideTrackerInactiveOutsdide() {
        UsageTracker.track(() -> {
            assertTrue(UsageTracker.isActive());

            Signal.untracked(() -> {
                assertFalse(UsageTracker.isActive());
                return null;
            });
        });

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
        Runnable cleanup = usage.onNextChange(() -> false);

        assertEquals(1, a.listeners.size());
        assertEquals(1, b.listeners.size());

        cleanup.run();
        assertEquals(0, a.listeners.size());
        assertEquals(0, b.listeners.size());
    }

    @Test
    void combinedOnNextChange_nonRepeatingListener_removedAfterFirstTrigger() {
        TestUsage a = new TestUsage();
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(() -> {
            count.incrementAndGet();
            return false;
        });

        boolean keep = a.listeners.get(0).invoke();
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
        usage.onNextChange(() -> {
            count.incrementAndGet();
            return true;
        });

        boolean keep = a.listeners.get(0).invoke();
        assertTrue(keep);
        assertEquals(1, count.intValue());

        assertEquals(1, a.listeners.size());
        assertEquals(1, b.listeners.size());
    }

    @Test
    void combinedOnNextChange_immediatelyNotifiedNonRepeatingListener_immediatelyNotifiedThenRemoved() {
        TestUsage a = new TestUsage() {
            @Override
            public Runnable onNextChange(TransientListener listener) {
                Runnable runnable = super.onNextChange(listener);
                listener.invoke();
                return runnable;
            }
        };
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(() -> {
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
            public Runnable onNextChange(TransientListener listener) {
                Runnable runnable = super.onNextChange(listener);
                listener.invoke();
                return runnable;
            }
        };
        TestUsage b = new TestUsage();
        AtomicInteger count = new AtomicInteger();

        CombinedUsage usage = new CombinedUsage(List.of(a, b));
        usage.onNextChange(() -> {
            count.incrementAndGet();
            return true;
        });

        assertEquals(1, count.intValue());

        assertEquals(1, a.listeners.size());
        assertEquals(1, b.listeners.size());
    }

    private static class TestUsage implements Usage {
        boolean hasChanges;
        List<TransientListener> listeners = new ArrayList<>();

        @Override
        public boolean hasChanges() {
            return hasChanges;
        }

        @Override
        public Runnable onNextChange(TransientListener listener) {
            listeners.add(listener);

            return () -> listeners.remove(listener);
        }
    }
}
