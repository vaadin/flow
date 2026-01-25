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
import com.vaadin.signals.Signal;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListSignalTest extends SignalTestBase {

    @Test
    void constructor_createsEmptyList() {
        ListSignal<String> list = new ListSignal<>();

        assertTrue(list.value().isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void insertFirst_addsToBeginning() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertFirst("first");
        ValueSignal<String> second = list.insertFirst("second");

        assertEquals(2, list.size());
        assertEquals("second", list.get(0).value());
        assertEquals("first", list.get(1).value());
        assertSame(second, list.get(0));
        assertSame(first, list.get(1));
    }

    @Test
    void insertLast_addsToEnd() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertLast("first");
        ValueSignal<String> second = list.insertLast("second");

        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value());
        assertEquals("second", list.get(1).value());
        assertSame(first, list.get(0));
        assertSame(second, list.get(1));
    }

    @Test
    void insertAt_insertsAtIndex() {
        ListSignal<String> list = new ListSignal<>();

        list.insertLast("first");
        list.insertLast("third");
        ValueSignal<String> middle = list.insertAt(1, "second");

        assertEquals(3, list.size());
        assertEquals("first", list.get(0).value());
        assertEquals("second", list.get(1).value());
        assertEquals("third", list.get(2).value());
        assertSame(middle, list.get(1));
    }

    @Test
    void insertAt_invalidIndex_throws() {
        ListSignal<String> list = new ListSignal<>();

        assertThrows(IndexOutOfBoundsException.class,
                () -> list.insertAt(1, "value"));
        assertThrows(IndexOutOfBoundsException.class,
                () -> list.insertAt(-1, "value"));
    }

    @Test
    void remove_removesChild() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertLast("first");
        ValueSignal<String> second = list.insertLast("second");

        boolean removed = list.remove(first);

        assertTrue(removed);
        assertEquals(1, list.size());
        assertSame(second, list.get(0));
    }

    @Test
    void remove_notInList_returnsFalse() {
        ListSignal<String> list = new ListSignal<>();
        ValueSignal<String> other = new ValueSignal<>("other");

        boolean removed = list.remove(other);

        assertFalse(removed);
    }

    @Test
    void removeAt_removesAtIndex() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertLast("first");
        ValueSignal<String> second = list.insertLast("second");
        ValueSignal<String> third = list.insertLast("third");

        ValueSignal<String> removed = list.removeAt(1);

        assertSame(second, removed);
        assertEquals(2, list.size());
        assertSame(first, list.get(0));
        assertSame(third, list.get(1));
    }

    @Test
    void removeAt_invalidIndex_throws() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");

        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
    }

    @Test
    void clear_removesAllItems() {
        ListSignal<String> list = new ListSignal<>();

        list.insertLast("first");
        list.insertLast("second");
        list.insertLast("third");

        list.clear();

        assertTrue(list.value().isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void clear_emptyList_doesNotNotify() {
        ListSignal<String> list = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        list.clear();

        assertFalse(usage.hasChanges());
    }

    @Test
    void get_returnsCorrectSignal() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertLast("first");
        ValueSignal<String> second = list.insertLast("second");

        assertSame(first, list.get(0));
        assertSame(second, list.get(1));
    }

    @Test
    void get_invalidIndex_throws() {
        ListSignal<String> list = new ListSignal<>();

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    @Test
    void moveTo_movesChild() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertLast("first");
        ValueSignal<String> second = list.insertLast("second");
        ValueSignal<String> third = list.insertLast("third");

        list.moveTo(first, 2);

        assertSame(second, list.get(0));
        assertSame(third, list.get(1));
        assertSame(first, list.get(2));
    }

    @Test
    void moveTo_notInList_throws() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");
        ValueSignal<String> other = new ValueSignal<>("other");

        assertThrows(IllegalArgumentException.class,
                () -> list.moveTo(other, 0));
    }

    @Test
    void moveTo_samePosition_doesNotNotify() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> first = list.insertLast("first");
        list.insertLast("second");

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        list.moveTo(first, 0);

        assertFalse(usage.hasChanges());
    }

    @Test
    void contains_returnsTrue_whenChildInList() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> child = list.insertLast("item");

        assertTrue(list.contains(child));
    }

    @Test
    void contains_returnsFalse_whenChildNotInList() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");
        ValueSignal<String> other = new ValueSignal<>("other");

        assertFalse(list.contains(other));
    }

    @Test
    void indexOf_returnsCorrectIndex() {
        ListSignal<String> list = new ListSignal<>();

        list.insertLast("first");
        ValueSignal<String> second = list.insertLast("second");
        list.insertLast("third");

        assertEquals(1, list.indexOf(second));
    }

    @Test
    void indexOf_notInList_returnsNegativeOne() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");
        ValueSignal<String> other = new ValueSignal<>("other");

        assertEquals(-1, list.indexOf(other));
    }

    @Test
    void value_returnsUnmodifiableList() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");

        List<ValueSignal<String>> value = list.value();

        assertThrows(UnsupportedOperationException.class, () -> value.clear());
    }

    @Test
    void peek_returnsUnmodifiableList() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");

        List<ValueSignal<String>> value = list.peek();

        assertThrows(UnsupportedOperationException.class, () -> value.clear());
    }

    @Test
    void usageTracker_insertDetected() {
        ListSignal<String> list = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        assertFalse(usage.hasChanges());

        AtomicBoolean invoked = new AtomicBoolean(false);
        usage.onNextChange(initial -> {
            assertFalse(initial);
            invoked.set(true);
            return false;
        });

        list.insertLast("item");

        assertTrue(usage.hasChanges());
        assertTrue(invoked.get());
    }

    @Test
    void usageTracker_removeDetected() {
        ListSignal<String> list = new ListSignal<>();
        ValueSignal<String> child = list.insertLast("item");

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        list.remove(child);

        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_clearDetected() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("item");

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        list.clear();

        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_moveDetected() {
        ListSignal<String> list = new ListSignal<>();
        ValueSignal<String> first = list.insertLast("first");
        list.insertLast("second");

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        list.moveTo(first, 1);

        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracker_peek_noUsageDetected() {
        ListSignal<String> list = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            list.peek();
        });

        assertSame(UsageTracker.NO_USAGE, usage);
    }

    @Test
    void usageTracker_keepListening_listenerKept() {
        ListSignal<String> list = new ListSignal<>();

        Usage usage = UsageTracker.track(() -> {
            list.value();
        });

        list.insertLast("first");

        AtomicInteger count = new AtomicInteger();
        usage.onNextChange(ignore -> {
            count.incrementAndGet();
            return true;
        });

        list.insertLast("second");
        assertEquals(2, count.get());

        list.insertLast("third");
        assertEquals(3, count.get());
    }

    @Test
    void transactions_useListSignalInTransaction_throws() {
        ListSignal<String> list = new ListSignal<>();

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                list.value();
            });
        });
    }

    @Test
    void transactions_modifyListSignalInTransaction_throws() {
        ListSignal<String> list = new ListSignal<>();

        assertThrows(IllegalStateException.class, () -> {
            Signal.runInTransaction(() -> {
                list.insertLast("item");
            });
        });
    }

    @Test
    void childValue_canBeModified() {
        ListSignal<String> list = new ListSignal<>();

        ValueSignal<String> child = list.insertLast("initial");
        child.value("updated");

        assertEquals("updated", child.value());
        assertEquals("updated", list.get(0).value());
    }

    @Test
    void toString_showsValues() {
        ListSignal<String> list = new ListSignal<>();
        list.insertLast("first");
        list.insertLast("second");

        String string = list.toString();

        assertEquals("ListSignal[first, second]", string);
    }

    @Test
    void toString_emptyList() {
        ListSignal<String> list = new ListSignal<>();

        assertEquals("ListSignal[]", list.toString());
    }
}
