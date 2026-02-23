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
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsageTrackerTest extends SignalTestBase {
    @Test
    void hasChanges_runInTransaction_readsFromTransaction() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        Signal.runInTransaction(() -> {
            signal.set("changed");

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
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.set("update");
        assertTrue(usage.hasChanges());
    }

    @Test
    void track_peekInCallback_notTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.peek();
        });

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void track_peekConfirmedInCallback_notTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            signal.peekConfirmed();
        });

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_useValue_notRegistered() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            Signal.untracked(() -> {
                signal.get();
                return null;
            });
        });

        signal.set("update");
        assertFalse(usage.hasChanges());
    }

    @Test
    void untracked_writeInCallback_allowedNoUsageTracked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        Usage usage = UsageTracker.track(() -> {
            Signal.untracked(() -> {
                signal.set("update");
                return null;
            });
        });

        signal.set("another");
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
    void get_outsideTrackingContext_throwsIllegalStateException() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        assertThrows(IllegalStateException.class, signal::get);
    }

    @Test
    void get_insideTrack_succeeds() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        UsageTracker.track(() -> {
            assertEquals("value", signal.get());
        });
    }

    @Test
    void get_insideUntracked_succeeds() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        assertEquals("value", Signal.untracked(() -> signal.get()));
    }

    @Test
    void get_insideUntrackedInsideTrack_succeeds() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        UsageTracker.track(() -> {
            assertEquals("value", Signal.untracked(() -> signal.get()));
        });
    }

    @Test
    void peek_outsideTrackingContext_succeeds() {
        ValueSignal<String> signal = new ValueSignal<>("value");
        assertEquals("value", signal.peek());
    }

    @Test
    void peek_onLambdaSignal_outsideTrackingContext_succeeds() {
        ValueSignal<String> signal = new ValueSignal<>("hello");

        Signal<String> mapped = signal.map(v -> v.toUpperCase());
        assertDoesNotThrow(() -> mapped.peek());
        assertEquals("HELLO", mapped.peek());

        Signal<String> readonly = signal.map(v -> v);
        assertDoesNotThrow(() -> readonly.peek());
        assertEquals("hello", readonly.peek());
    }

    @Test
    void isGetAllowed_outsideContext_false() {
        assertFalse(UsageTracker.isGetAllowed());
    }

    @Test
    void isGetAllowed_insideTrack_true() {
        UsageTracker.track(() -> {
            assertTrue(UsageTracker.isGetAllowed());
        });
    }

    @Test
    void isGetAllowed_insideUntracked_true() {
        Signal.untracked(() -> {
            assertTrue(UsageTracker.isGetAllowed());
            return null;
        });
    }

    @Test
    void isGetAllowed_insideUntrackedInsideTrack_true() {
        UsageTracker.track(() -> {
            Signal.untracked(() -> {
                assertTrue(UsageTracker.isGetAllowed());
                return null;
            });
        });
    }

    @Test
    void isActive_insideUntracked_false() {
        Signal.untracked(() -> {
            assertFalse(UsageTracker.isActive());
            return null;
        });
    }

    @Test
    void get_sharedSignal_outsideTrackingContext_throws() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");
        assertThrows(IllegalStateException.class, signal::get);
    }

    @Test
    void peek_sharedSignal_outsideTrackingContext_succeeds() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");
        assertEquals("value", signal.peek());
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
