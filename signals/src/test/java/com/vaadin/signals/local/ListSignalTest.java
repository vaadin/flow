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
package com.vaadin.signals.local;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListSignalTest extends SignalTestBase {

    @Test
    void constructor_empty_emptyList() {
        ListSignal<String> signal = new ListSignal<>();

        List<ValueSignal<String>> value = signal.value();

        assertTrue(value.isEmpty());
    }

    @Test
    void insertFirst_singleValue_valueAtStart() {
        ListSignal<String> signal = new ListSignal<>();

        ValueSignal<String> entry = signal.insertFirst("first");

        assertEquals("first", entry.value());
        assertValues(signal, "first");
    }

    @Test
    void insertFirst_twoValues_insertedInOrder() {
        ListSignal<String> signal = new ListSignal<>();

        signal.insertFirst("a");
        signal.insertFirst("b");

        assertValues(signal, "b", "a");
    }

    @Test
    void insertLast_singleValue_valueAtEnd() {
        ListSignal<String> signal = new ListSignal<>();

        ValueSignal<String> entry = signal.insertLast("last");

        assertEquals("last", entry.value());
        assertValues(signal, "last");
    }

    @Test
    void insertLast_twoValues_insertedInOrder() {
        ListSignal<String> signal = new ListSignal<>();

        signal.insertLast("a");
        signal.insertLast("b");

        assertValues(signal, "a", "b");
    }

    @Test
    void insertAt_validIndex_valueInserted() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("first");
        signal.insertLast("last");

        ValueSignal<String> entry = signal.insertAt(1, "middle");

        assertEquals("middle", entry.value());
        assertValues(signal, "first", "middle", "last");
    }

    @Test
    void insertAt_atZero_valueInsertedAtStart() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("existing");

        signal.insertAt(0, "first");

        assertValues(signal, "first", "existing");
    }

    @Test
    void insertAt_atEnd_valueInsertedAtEnd() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("existing");

        signal.insertAt(1, "last");

        assertValues(signal, "existing", "last");
    }

    @Test
    void insertAt_negativeIndex_throwsException() {
        ListSignal<String> signal = new ListSignal<>();

        assertThrows(IndexOutOfBoundsException.class,
                () -> signal.insertAt(-1, "value"));
    }

    @Test
    void insertAt_indexTooLarge_throwsException() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("existing");

        assertThrows(IndexOutOfBoundsException.class,
                () -> signal.insertAt(2, "value"));
    }

    @Test
    void insert_returnsEntrySignal() {
        ListSignal<String> signal = new ListSignal<>();

        ValueSignal<String> entry = signal.insertLast("value");

        assertEquals("value", entry.value());
        entry.value("updated");
        assertEquals("updated", entry.value());
    }

    @Test
    void remove_existingEntry_entryRemoved() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("first");
        ValueSignal<String> toRemove = signal.insertLast("second");
        signal.insertLast("third");

        signal.remove(toRemove);

        assertValues(signal, "first", "third");
    }

    @Test
    void remove_nonExistentEntry_noChange() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("value");
        ValueSignal<String> other = new ValueSignal<>("other");

        signal.remove(other);

        assertValues(signal, "value");
    }

    @Test
    void clear_listWithEntries_listCleared() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("a");
        signal.insertLast("b");
        signal.insertLast("c");

        signal.clear();

        assertTrue(signal.value().isEmpty());
    }

    @Test
    void value_returnsImmutableList() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("value");

        List<ValueSignal<String>> value = signal.value();

        assertThrows(UnsupportedOperationException.class, () -> {
            value.add(new ValueSignal<>("new"));
        });
    }

    @Test
    void peek_returnsValueWithoutTracking() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("value");

        Usage usage = UsageTracker.track(() -> {
            signal.peek();
        });

        assertSame(UsageTracker.NO_USAGE, usage);
    }

    @Test
    void value_changeAfterSnapshot_snapshotUnchanged() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("first");

        List<ValueSignal<String>> snapshot = signal.value();

        signal.insertFirst("second");

        assertEquals(1, snapshot.size());
        assertEquals("first", snapshot.get(0).value());
    }

    @Test
    void modifyEntry_onlyThatEntryNotified() {
        ListSignal<String> signal = new ListSignal<>();
        ValueSignal<String> entry1 = signal.insertLast("a");
        ValueSignal<String> entry2 = signal.insertLast("b");

        AtomicBoolean entry1Changed = new AtomicBoolean(false);
        AtomicBoolean entry2Changed = new AtomicBoolean(false);
        AtomicBoolean listChanged = new AtomicBoolean(false);

        Usage usage1 = UsageTracker.track(entry1::value);
        Usage usage2 = UsageTracker.track(entry2::value);
        Usage listUsage = UsageTracker.track(signal::value);

        usage1.onNextChange(initial -> {
            entry1Changed.set(true);
            return false;
        });
        usage2.onNextChange(initial -> {
            entry2Changed.set(true);
            return false;
        });
        listUsage.onNextChange(initial -> {
            listChanged.set(true);
            return false;
        });

        entry1.value("updated");

        assertTrue(entry1Changed.get());
        assertFalse(entry2Changed.get());
        assertFalse(listChanged.get());
    }

    @Test
    void usageTracker_insertValue_changeDetected() {
        ListSignal<String> signal = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        assertFalse(usage.hasChanges());

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        signal.insertLast("value");

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
    }

    @Test
    void usageTracker_removeValue_changeDetected() {
        ListSignal<String> signal = new ListSignal<>();
        ValueSignal<String> entry = signal.insertLast("value");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        assertFalse(usage.hasChanges());

        signal.remove(entry);

        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_clear_changeDetected() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("value");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        assertFalse(usage.hasChanges());

        signal.clear();

        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_modifyEntry_listNotChanged() {
        ListSignal<String> signal = new ListSignal<>();
        ValueSignal<String> entry = signal.insertLast("value");

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        entry.value("updated");

        assertFalse(usage.hasChanges());
    }

    @Test
    void usageTracker_peek_noUsageDetected() {
        ListSignal<String> signal = new ListSignal<>();
        signal.insertLast("value");

        Usage usage = UsageTracker.track(() -> {
            signal.peek();
        });

        assertSame(UsageTracker.NO_USAGE, usage);
    }

    @Test
    void usageTracker_listener_keepListening() {
        ListSignal<String> signal = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.insertLast("update1");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return true;
        });

        // Verify preserving after initial and trigger subsequent update
        signal.insertLast("update2");
        assertEquals(2, count.get());

        // Verify subsequent update
        signal.insertLast("update3");
        assertEquals(3, count.get());
    }

    @Test
    void usageTracker_listener_stopAfterFirst() {
        ListSignal<String> signal = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.insertLast("update1");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return false;
        });

        assertEquals(1, count.intValue());

        signal.insertLast("update2");
        assertEquals(1, count.intValue());
    }

    private static void assertValues(ListSignal<String> signal,
            String... expectedValues) {
        List<String> values = signal.value().stream().map(ValueSignal::value)
                .toList();
        assertEquals(List.of(expectedValues), values);
    }
}
