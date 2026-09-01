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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A row is a typed container of cells, so it inherits
 * {@link com.vaadin.flow.component.HasComponentsOfType#bindChildren} and can
 * follow a list signal the same way a section can.
 */
class TableRowBindChildrenTest extends SignalsUnitTest {

    @Test
    void bindChildren_followsTheListSignal() {
        TableRow row = new TableRow();
        UI.getCurrent().add(row);
        ListSignal<String> values = new ListSignal<>();

        row.bindChildren(values, TableDataCell::new);

        assertEquals(List.of(), row.getCells());

        values.insertLast("0.330");
        values.insertLast("4,879");

        assertEquals(List.of("0.330", "4,879"), row.getDataCells().stream()
                .map(TableDataCell::getText).toList());
    }

    @Test
    void mutatingWhileBound_throws() {
        TableRow row = new TableRow();
        UI.getCurrent().add(row);
        ListSignal<String> values = new ListSignal<>();
        row.bindChildren(values, TableDataCell::new);

        // the three ways of attaching a cell all go through the inherited add
        // or addComponentAtIndex, so they pick up the guard the contract
        // applies: a factory, an insert, and addCells wrapping a non-cell
        assertThrows(BindingActiveException.class, row::addDataCell);
        assertThrows(BindingActiveException.class, () -> row.insertDataCell(0));
        Span loose = new Span("x");
        assertThrows(BindingActiveException.class, () -> row.addCells(loose));
    }
}
