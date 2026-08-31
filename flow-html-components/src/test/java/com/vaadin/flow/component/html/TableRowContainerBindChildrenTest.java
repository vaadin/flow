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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @ParameterizedTest
    @MethodSource("sections")
    void mutatingWhileBound_throws(Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        UI.getCurrent().add((com.vaadin.flow.component.Component) section);
        ListSignal<String> planets = new ListSignal<>();
        section.bindChildren(planets,
                signal -> new TableRow(new TableDataCell(signal)));

        // the row factories go through the inherited add, so the binding is
        // protected from being edited behind its back
        assertThrows(BindingActiveException.class, section::addRow);
        assertThrows(BindingActiveException.class, () -> section.insertRow(0));
        assertThrows(BindingActiveException.class,
                () -> section.add(new TableRow()));
    }

    @ParameterizedTest
    @MethodSource("sections")
    void bindChildren_twice_throws(Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        UI.getCurrent().add((com.vaadin.flow.component.Component) section);
        ListSignal<String> planets = new ListSignal<>();
        section.bindChildren(planets, signal -> new TableRow());

        assertThrows(BindingActiveException.class, () -> section
                .bindChildren(new ListSignal<>(), signal -> new TableRow()));
    }
}
