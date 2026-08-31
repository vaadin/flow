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
        group.addColumns(List.of(third));

        assertEquals(List.of(first, second, third), group.getColumns());
    }

    @Test
    void spanAndColumns_areMutuallyExclusive() {
        TableColumnGroup spanning = group();
        spanning.setSpan(3);
        assertThrows(IllegalStateException.class, spanning::addColumn);
        assertThrows(IllegalStateException.class,
                () -> spanning.addColumns(new TableColumn()));

        TableColumnGroup withColumns = new TableColumnGroup();
        withColumns.addColumn();
        assertThrows(IllegalStateException.class, () -> withColumns.setSpan(3));
    }

    @Test
    void resettingOneModeAllowsTheOther() {
        TableColumnGroup group = group();
        group.setSpan(3);

        group.resetSpan();
        TableColumn column = group.addColumn();

        assertEquals(List.of(column), group.getColumns());

        group.removeAllColumns();
        group.setSpan(2);

        assertEquals(2, group.getSpan());
    }

    @Test
    void removeColumn_detachesOnlyThatColumn() {
        TableColumnGroup group = group();
        TableColumn first = group.addColumn();
        TableColumn second = group.addColumn();

        group.removeColumn(first);

        assertEquals(List.of(second), group.getColumns());
        assertTrue(first.getParent().isEmpty());
    }

    @Test
    void removeAllColumns_leavesTheGroupEmpty() {
        TableColumnGroup group = group();
        group.addColumn();
        group.addColumn();

        group.removeAllColumns();

        assertTrue(group.getColumns().isEmpty());
    }
}
