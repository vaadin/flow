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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the constructors both {@link TableCell} subclasses inherit, through
 * each of them.
 */
class TableCellTest extends SignalsUnitTest {

    static Stream<Named<Function<Component[], TableCell>>> childrenConstructors() {
        return Stream.of(Named.of("td", TableDataCell::new),
                Named.of("th", TableHeaderCell::new));
    }

    @ParameterizedTest
    @MethodSource("childrenConstructors")
    void childrenConstructor_addsGivenChildren(
            Function<Component[], TableCell> constructor) {
        Span span = new Span("a");
        Paragraph paragraph = new Paragraph("b");

        TableCell cell = constructor.apply(new Component[] { span, paragraph });

        assertEquals(List.of(span, paragraph), cell.getChildren().toList());
    }

    static Stream<Named<Function<String, TableCell>>> textConstructors() {
        return Stream.of(Named.of("td", TableDataCell::new),
                Named.of("th", TableHeaderCell::new));
    }

    @ParameterizedTest
    @MethodSource("textConstructors")
    void textConstructor_setsTheText(Function<String, TableCell> constructor) {
        TableCell cell = constructor.apply("content");

        assertEquals("content", cell.getText());
    }

    static Stream<Named<Function<Signal<String>, TableCell>>> signalConstructors() {
        return Stream.of(Named.of("td", TableDataCell::new),
                Named.of("th", TableHeaderCell::new));
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
}
