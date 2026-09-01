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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The row containers inherit
 * {@link com.vaadin.flow.component.HasComponentsOfType#bindChildren} from the
 * typed container contract, so a table section can follow a list signal.
 */
class TableRowContainerBindChildrenTest extends SignalsUnitTest {

    static Stream<Named<Supplier<TableRowContainer>>> sections() {
        return Stream.of(Named.of("thead", TableHead::new),
                Named.of("tbody", TableBody::new),
                Named.of("tfoot", TableFoot::new));
    }

    @ParameterizedTest
    @MethodSource("sections")
    void bindChildren_followsTheListSignal(
            Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        UI.getCurrent().add((com.vaadin.flow.component.Component) section);
        ListSignal<String> planets = new ListSignal<>();

        section.bindChildren(planets,
                signal -> new TableRow(new TableDataCell(signal)));

        assertEquals(List.of(), section.getRows());

        planets.insertLast("Mercury");
        planets.insertLast("Venus");

        assertEquals(List.of("Mercury", "Venus"), section.getRows().stream()
                .map(row -> row.getDataCells().get(0).getText()).toList());
    }

    @Test
    void bindChildren_followsRemovalsReordersAndValueChanges() {
        Table table = new Table();
        UI.getCurrent().add(table);
        ListSignal<String> planets = new ListSignal<>();
        table.getBody().bindChildren(planets,
                signal -> new TableRow(new TableDataCell(signal)));

        var mercury = planets.insertLast("Mercury");
        var venus = planets.insertLast("Venus");
        var earth = planets.insertLast("Earth");

        assertEquals(List.of("Mercury", "Venus", "Earth"), texts(table));

        // an item signal's value reaches the cell that was bound to it
        venus.set("Venus II");
        assertEquals(List.of("Mercury", "Venus II", "Earth"), texts(table));

        planets.moveTo(earth, 0);
        assertEquals(List.of("Earth", "Mercury", "Venus II"), texts(table));

        planets.remove(mercury);
        assertEquals(List.of("Earth", "Venus II"), texts(table));

        planets.clear();
        assertEquals(List.of(), texts(table));
        assertTrue(table.getAllRows().isEmpty());
    }

    private static List<String> texts(Table table) {
        return table.getBodyRows().stream()
                .map(row -> row.getDataCells().get(0).getText()).toList();
    }

    @ParameterizedTest
    @MethodSource("sections")
    void mutatingWhileBound_throws(Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        UI.getCurrent().add((com.vaadin.flow.component.Component) section);
        ListSignal<String> planets = new ListSignal<>();
        section.bindChildren(planets,
                signal -> new TableRow(new TableDataCell(signal)));

        // the row factories go through the inherited add and
        // addComponentAtIndex rather than the element, so they pick up the
        // guard the contract already applies
        assertThrows(BindingActiveException.class, section::addRow);
        assertThrows(BindingActiveException.class, () -> section.insertRow(0));
    }
}
