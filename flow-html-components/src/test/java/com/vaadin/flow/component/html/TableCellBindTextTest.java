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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the signal-bound text of both {@link TableCell} subclasses.
 */
class TableCellBindTextTest extends SignalsUnitTest {

    static Stream<Named<Function<Signal<String>, TableCell>>> signalConstructors() {
        return Stream.of(Named.of("td", TableDataCell::new),
                Named.of("th", TableHeaderCell::new));
    }

    /**
     * The row factories that take a signal, paired with the scope each is
     * expected to put on the cell it creates.
     */
    static Stream<Named<SignalFactory>> signalFactories() {
        return Stream.of(
                Named.of("addDataCell",
                        new SignalFactory(TableRow::addDataCell, null)),
                Named.of("addHeaderCell",
                        new SignalFactory(TableRow::addHeaderCell, null)),
                Named.of("addRowHeaderCell",
                        new SignalFactory(TableRow::addRowHeaderCell,
                                TableHeaderCell.Scope.ROW)),
                Named.of("addColumnHeaderCell",
                        new SignalFactory(TableRow::addColumnHeaderCell,
                                TableHeaderCell.Scope.COL)),
                Named.of("addRowGroupHeaderCell",
                        new SignalFactory(TableRow::addRowGroupHeaderCell,
                                TableHeaderCell.Scope.ROWGROUP)),
                Named.of("addColumnGroupHeaderCell",
                        new SignalFactory(TableRow::addColumnGroupHeaderCell,
                                TableHeaderCell.Scope.COLGROUP)));
    }

    record SignalFactory(
            BiFunction<TableRow, Signal<String>, ? extends TableCell> create,
            TableHeaderCell.Scope scope) {
    }

    @ParameterizedTest
    @MethodSource("signalFactories")
    void signalFactories_bindTheTextAndKeepTheScope(SignalFactory factory) {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        TableRow row = new TableRow();
        UI.getCurrent().add(row);

        TableCell cell = factory.create().apply(row, signal);

        assertEquals(List.of(cell), row.getCells());
        assertEquals("initial", cell.getText());
        signal.set("updated");
        assertEquals("updated", cell.getText());
        assertEquals(Optional.ofNullable(factory.scope()),
                cell instanceof TableHeaderCell header ? header.getScope()
                        : Optional.empty());
    }

    @Test
    void caption_signalConstructor_bindsTheText() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        TableCaption caption = new TableCaption(signal);
        Table table = new Table();
        table.setCaption(caption);
        UI.getCurrent().add(table);

        assertEquals("initial", table.getCaptionText());
        signal.set("updated");
        assertEquals("updated", table.getCaptionText());
    }

    @ParameterizedTest
    @MethodSource("signalConstructors")
    void signalConstructor_bindsTheText(
            Function<Signal<String>, TableCell> constructor) {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        TableCell cell = constructor.apply(signal);
        UI.getCurrent().add(cell);

        assertEquals("initial", cell.getText());
        signal.set("updated");
        assertEquals("updated", cell.getText());
    }

    @ParameterizedTest
    @MethodSource("signalConstructors")
    void signalConstructor_nullSignal_throws(
            Function<Signal<String>, TableCell> constructor) {
        assertThrows(NullPointerException.class, () -> constructor.apply(null));
    }

    @ParameterizedTest
    @MethodSource("signalConstructors")
    void bindText_setTextWhileBindingActive_throws(
            Function<Signal<String>, TableCell> constructor) {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        TableCell cell = constructor.apply(signal);
        UI.getCurrent().add(cell);

        assertThrows(BindingActiveException.class,
                () -> cell.setText("manual"));
    }
}
