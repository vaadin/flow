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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void getRow() {
        var row0 = new NativeTableRow();
        var row1 = new NativeTableRow();
        var row2 = new NativeTableRow();
        container.addRows(row0, row1, row2);
        AssertUtils.assertEquals(row0, container.getRow(0).orElseThrow(),
                "Row 0 does not match");
        AssertUtils.assertEquals(row1, container.getRow(1).orElseThrow(),
                "Row 1 does not match");
        AssertUtils.assertEquals(row2, container.getRow(2).orElseThrow(),
                "Row 2 does not match");
    }

    @Test
    void getNonExistentRow() {
        container.addRow();
        assertTrue(container.getRow(0).isPresent());
        assertTrue(container.getRow(1).isEmpty());
    }

    @Test
    void getRowIndex() {
        for (int i = 0; i < 10; i++) {
            container.addRow();
            var row = container.getRow(i).orElseThrow();
            int rowIndex = container.getRowIndex(row);
            assertEquals(i, rowIndex);
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
    void removeRowByIndex() {
        var row0 = container.addRow();
        var row1 = container.addRow();
        var row2 = container.addRow();
        container.removeRow(1);
        assertTrue(row1.getParent().isEmpty());
        var children = container.getChildren().toList();
        assertEquals(2, children.size());
        AssertUtils.assertEquals(row0, children.get(0),
                "row0 must not be removed");
        AssertUtils.assertEquals(row2, children.get(1),
                "row2 must not be removed");
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
        AssertUtils.assertEquals(newRow, container.getRow(1).orElseThrow(),
                "Row must be replaced with new row");
    }

    @Test
    void getRowCount() {
        assertEquals(0, container.getRowCount());
        container.addRow();
        assertEquals(1, container.getRowCount());
        container.addRow();
        assertEquals(2, container.getRowCount());
        container.removeRow(0);
        assertEquals(1, container.getRowCount());
    }

    @Tag(Tag.TR)
    static class RowContainer extends Component
            implements NativeTableRowContainer {
    }

}
