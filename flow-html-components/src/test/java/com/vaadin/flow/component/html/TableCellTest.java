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
import java.util.function.Supplier;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    static Stream<Named<Supplier<TableCell>>> cellKinds() {
        return Stream.of(Named.of("td", TableDataCell::new),
                Named.of("th", TableHeaderCell::new));
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void spansDefaultToOneAndReadBackWhatIsSet(Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        assertEquals(1, cell.getColspan());
        assertEquals(1, cell.getRowspan());

        cell.setColspan(2);
        cell.setRowspan(3);

        assertEquals("2", cell.getElement().getAttribute("colspan"));
        assertEquals("3", cell.getElement().getAttribute("rowspan"));
        assertEquals(2, cell.getColspan());
        assertEquals(3, cell.getRowspan());
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void resetSpans_removeTheAttributes(Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        cell.setColspan(2);
        cell.setRowspan(3);

        cell.resetColspan();
        cell.resetRowspan();

        assertNull(cell.getElement().getAttribute("colspan"));
        assertNull(cell.getElement().getAttribute("rowspan"));
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void spans_rejectNegativeValues(Supplier<TableCell> factory) {
        TableCell cell = factory.get();

        assertEquals("colspan must be a positive integer value",
                assertThrows(IllegalArgumentException.class,
                        () -> cell.setColspan(-1)).getMessage());
        assertEquals("rowspan must be a non-negative integer value",
                assertThrows(IllegalArgumentException.class,
                        () -> cell.setRowspan(-1)).getMessage());
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void zeroSpan_rejectedForColumnsButNotForRows(Supplier<TableCell> factory) {
        TableCell cell = factory.get();

        // colspan=0 was dropped from HTML and browsers clamp it back to 1
        assertThrows(IllegalArgumentException.class, () -> cell.setColspan(0));

        // rowspan=0 still means "to the end of the row group"
        cell.setRowspan(0);
        assertEquals("0", cell.getElement().getAttribute("rowspan"));
        assertEquals(0, cell.getRowspan());
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void setHeaders_writesSpaceJoinedAttributeAndReadsBack(
            Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        assertTrue(cell.getHeaders().isEmpty());

        cell.setHeaders("name", "age");

        assertEquals("name age", cell.getElement().getAttribute("headers"));
        assertEquals(List.of("name", "age"), cell.getHeaders());
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void setHeaders_listEntryPointBehavesLikeTheVarargsOne(
            Supplier<TableCell> factory) {
        TableCell cell = factory.get();

        cell.setHeaders(List.of("name", "age"));

        assertEquals(List.of("name", "age"), cell.getHeaders());
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void setHeaders_empty_clearsTheAttribute(Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        cell.setHeaders("name");

        cell.setHeaders(new String[0]);

        assertNull(cell.getElement().getAttribute("headers"));
        assertTrue(cell.getHeaders().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void setHeaders_fromHeaderCells_usesTheirIds(Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        TableHeaderCell name = new TableHeaderCell("Name");
        TableHeaderCell age = new TableHeaderCell("Age");
        name.setId("name-h");
        age.setId("age-h");

        cell.setHeaders(name, age);

        assertEquals("name-h age-h", cell.getElement().getAttribute("headers"));

        cell.setHeadersByCells(List.of(age, name));

        assertEquals("age-h name-h", cell.getElement().getAttribute("headers"));
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void setHeaders_fromHeaderCellWithoutId_throws(
            Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        TableHeaderCell withoutId = new TableHeaderCell("Name");

        assertThrows(IllegalArgumentException.class,
                () -> cell.setHeaders(withoutId));
    }

    @ParameterizedTest
    @MethodSource("cellKinds")
    void resetHeaders_removesTheAttribute(Supplier<TableCell> factory) {
        TableCell cell = factory.get();
        cell.setHeaders("name");

        cell.resetHeaders();

        assertNull(cell.getElement().getAttribute("headers"));
        assertTrue(cell.getHeaders().isEmpty());
    }
}
