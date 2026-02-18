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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.impl.UsageTracker.CombinedUsage;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsageTrackerTest extends SignalTestBase {
    @Test
    void hasChanges_runInTransaction_readsFromTransaction() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            signal.get();
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

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
        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        assertFalse(usage.hasChanges());
    }

    @Test
    void track_readValueInCallback_tracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            signal.get();
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        signal.set("update");
        assertTrue(usage.hasChanges());
    }

    @Test
    void track_peekInCallback_notTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            signal.peek();
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_peekConfirmedInCallback_notTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            signal.peekConfirmed();
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_useValue_notRegistered() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            Signal.untracked(() -> {
                signal.get();
                return null;
            });
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_writeInCallback_allowedNoUsageTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            signal.set("update");
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        signal.set("another");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_multipleUsages_combinedUsage() {
        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(new TestUsage());
            UsageTracker.registerUsage(new TestUsage());
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        assertInstanceOf(CombinedUsage.class, usage);
    }

    @Test
    void track_singleUsage_notCombinedUsage() {
        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(new TestUsage());
            return null;
        }, usageDetector).supply();
        Usage usage = usageDetector.dependencies();

        assertFalse(usage instanceof CombinedUsage);
    }

    @Test
    void isActive_activeInsideTrackerInactiveOutsdide() {
        UsageDetector usageDetector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            assertTrue(UsageTracker.isActive());
            Signal.untracked(() -> {
                assertFalse(UsageTracker.isActive());
                return null;
            });
            return null;
        }, usageDetector).supply();

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

    private static class TestUsage implements Usage {
        boolean hasChanges;
        List<TransientListener> listeners = new ArrayList<>();

        @Override
        public boolean hasChanges() {
            return hasChanges;
        }

        @Override
        public Registration onNextChange(TransientListener listener) {
            listeners.add(listener);

            return () -> listeners.remove(listener);
        }
    }
}
