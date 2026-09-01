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
package com.vaadin.flow.component.html;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.signals.local.ListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableColumnGroupTest extends ComponentTest {
    // Property tests in super class

    @Override
    protected void addProperties() {
        addProperty("span", int.class, 1, 2, false, false);
    }

    private TableColumnGroup group() {
        return (TableColumnGroup) getComponent();
    }

    @Test
    void addColumn_appendsAChild() {
        TableColumnGroup group = group();

        TableColumn plain = group.addColumn();
        TableColumn spanning = group.addColumn(2);

        assertEquals(List.of(plain, spanning), group.getColumns());
        assertEquals(1, plain.getSpan());
        assertEquals(2, spanning.getSpan());
    }

    static Stream<Named<Function<List<TableColumn>, TableColumnGroup>>> groupConstructors() {
        return Stream.of(
                Named.of("varargs",
                        c -> new TableColumnGroup(
                                c.toArray(TableColumn[]::new))),
                Named.of("list", TableColumnGroup::new));
    }

    @ParameterizedTest
    @MethodSource("groupConstructors")
    void constructorAndAddColumns_attachPreBuiltColumns(
            Function<List<TableColumn>, TableColumnGroup> constructor) {
        TableColumn first = new TableColumn();
        TableColumn second = new TableColumn(3);
        TableColumn third = new TableColumn();

        TableColumnGroup group = constructor.apply(List.of(first, second));
        group.add(third);

        assertEquals(List.of(first, second, third), group.getColumns());
    }

    @Test
    void spanAndColumns_areMutuallyExclusive() {
        TableColumnGroup spanning = group();
        spanning.setSpan(3);
        assertThrows(IllegalStateException.class, spanning::addColumn);
        assertThrows(IllegalStateException.class,
                () -> spanning.insertColumn(0));
        assertThrows(IllegalStateException.class,
                () -> spanning.add(new TableColumn()));

        TableColumnGroup withColumns = new TableColumnGroup();
        withColumns.addColumn();
        assertThrows(IllegalStateException.class, () -> withColumns.setSpan(3));
    }

    @Test
    void everyInheritedMutatorRespectsTheSpanMode() {
        TableColumnGroup group = group();
        group.setSpan(3);
        TableColumn column = new TableColumn();

        // the whole container contract has to honour the invariant, not just
        // the factory methods
        assertThrows(IllegalStateException.class,
                () -> group.addComponentAtIndex(0, column));
        assertThrows(IllegalStateException.class,
                () -> group.addComponentAsFirst(column));
        assertThrows(IllegalStateException.class,
                () -> group.replace(column, new TableColumn()));
        // bindChildren cannot honour the invariant, so it is refused outright
        assertThrows(UnsupportedOperationException.class,
                () -> group.bindChildren(new ListSignal<TableColumn>(),
                        signal -> new TableColumn()));
        assertTrue(group.getColumns().isEmpty());

        // and once the span is gone, they all work as the contract says
        group.resetSpan();
        TableColumn first = new TableColumn();
        TableColumn second = new TableColumn();
        group.addComponentAtIndex(0, second);
        group.addComponentAsFirst(first);
        assertEquals(List.of(first, second), group.getColumns());

        TableColumn replacement = new TableColumn();
        group.replace(second, replacement);
        assertEquals(List.of(first, replacement), group.getColumns());
    }

    @Test
    void resettingOneModeAllowsTheOther() {
        TableColumnGroup group = group();
        group.setSpan(3);

        group.resetSpan();
        TableColumn column = group.addColumn();

        assertEquals(List.of(column), group.getColumns());

        group.removeAll();
        group.setSpan(2);

        assertEquals(2, group.getSpan());
    }

    @Test
    void insertColumn_placesTheColumnAtTheGivenPosition() {
        TableColumnGroup group = new TableColumnGroup();
        TableColumn first = group.addColumn();
        TableColumn last = group.addColumn();

        TableColumn middle = group.insertColumn(1);

        assertEquals(List.of(first, middle, last), group.getColumns());
    }

    @Test
    void removeColumn_detachesOnlyThatColumn() {
        TableColumnGroup group = group();
        TableColumn first = group.addColumn();
        TableColumn second = group.addColumn();

        group.remove(first);

        assertEquals(List.of(second), group.getColumns());
        assertTrue(first.getParent().isEmpty());
    }

    @Test
    void removeAll_leavesTheGroupEmpty() {
        TableColumnGroup group = group();
        group.addColumn();
        group.addColumn();

        group.removeAll();

        assertTrue(group.getColumns().isEmpty());
    }
}
