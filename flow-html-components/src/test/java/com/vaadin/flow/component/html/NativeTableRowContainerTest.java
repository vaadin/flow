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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeTableRowContainerTest {

    private RowContainer container;

    @BeforeEach
    void setUp() {
        container = new RowContainer();
    }

    @Test
    void addRow() {
        var children = container.getChildren().toList();
        assertEquals(0, children.size());
        var row = container.addRow();
        children = container.getChildren().toList();
        assertEquals(1, children.size());
        AssertUtils.assertEquals(children.get(0), row,
                "Child is not added row");
        row = container.addRow();
        children = container.getChildren().toList();
        assertEquals(2, children.size());
        AssertUtils.assertEquals(children.get(1), row,
                "Child is not added row");
        for (var child : children) {
            assertTrue(child instanceof NativeTableRow);
        }
    }

    @Test
    void getRows() {
        for (int i = 0; i < 10; i++) {
            container.addRow();
        }
        var rows = container.getRows();
        var children = container.getChildren().toList();
        for (int i = 0; i < 10; i++) {
            AssertUtils.assertEquals(children.get(i), rows.get(i),
                    "row does not match");
        }
    }

    @Test
    void insertRow() {
        var row0 = new NativeTableRow();
        var row1 = new NativeTableRow();
        var row2 = new NativeTableRow();
        container.addRows(row0, row1, row2);
        var newRow = container.insertRow(1);
        var children = container.getChildren().toList();
        assertEquals(4, children.size());
        AssertUtils.assertEquals(newRow, children.get(1),
                "New row must be inserted at given position");
    }

    @Test
    void removeAllRows() {
        container.addRow();
        container.addRow();
        container.addRow();
        container.removeAllRows();
        assertEquals(0, container.getChildren().count());
    }

    @Test
    void removeRowsByReference() {
        var row0 = container.addRow();
        var row1 = container.addRow();
        var row2 = container.addRow();
        var row3 = container.addRow();
        var row4 = container.addRow();
        container.removeRows(row1, row3);
        assertTrue(row1.getParent().isEmpty());
        assertTrue(row3.getParent().isEmpty());
        var children = container.getChildren().toList();
        assertEquals(3, children.size());
        AssertUtils.assertEquals(container, row0.getParent().orElseThrow(),
                "row0 must not be removed");
        AssertUtils.assertEquals(container, row2.getParent().orElseThrow(),
                "row2 must not be removed");
        AssertUtils.assertEquals(container, row4.getParent().orElseThrow(),
                "row4 must not be removed");
    }

    @Test
    void replaceRow() {
        container.addRow();
        container.addRow();
        container.addRow();
        var newRow = new NativeTableRow();
        container.replaceRow(1, newRow);
        assertEquals(3, container.getChildren().count());
        AssertUtils.assertEquals(newRow, container.getRows().get(1),
                "Row must be replaced with new row");
    }

    static Stream<Named<Function<NativeTableRow[], NativeTableRowContainer>>> sectionConstructors() {
        return Stream.of(Named.of("thead", NativeTableHeader::new),
                Named.of("tbody", NativeTableBody::new),
                Named.of("tfoot", NativeTableFooter::new));
    }

    @ParameterizedTest
    @MethodSource("sectionConstructors")
    void varargsConstructor_addsGivenRows(
            Function<NativeTableRow[], NativeTableRowContainer> constructor) {
        var row0 = new NativeTableRow();
        var row1 = new NativeTableRow();

        var section = constructor.apply(new NativeTableRow[] { row0, row1 });

        assertEquals(List.of(row0, row1), section.getRows());
    }

    @Tag(Tag.TR)
    static class RowContainer extends Component
            implements NativeTableRowContainer {
    }

}
