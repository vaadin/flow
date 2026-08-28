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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@link TableRowContainer} default methods through each of the
 * three components implementing them.
 */
class TableRowContainerTest {

    static Stream<Named<Supplier<TableRowContainer>>> sections() {
        return Stream.of(Named.of("thead", TableHead::new),
                Named.of("tbody", TableBody::new),
                Named.of("tfoot", TableFoot::new));
    }

    static Stream<Named<Function<List<TableRow>, TableRowContainer>>> sectionConstructors() {
        return Stream.of(
                Named.of("thead",
                        rows -> new TableHead(rows.toArray(TableRow[]::new))),
                Named.of("tbody",
                        rows -> new TableBody(rows.toArray(TableRow[]::new))),
                Named.of("tfoot",
                        rows -> new TableFoot(rows.toArray(TableRow[]::new))));
    }

    @ParameterizedTest
    @MethodSource("sectionConstructors")
    void constructor_addsGivenRows(
            Function<List<TableRow>, TableRowContainer> constructor) {
        TableRow row0 = new TableRow();
        TableRow row1 = new TableRow();

        TableRowContainer section = constructor.apply(List.of(row0, row1));

        assertEquals(List.of(row0, row1), section.getRows());
    }

    @ParameterizedTest
    @MethodSource("sections")
    void addRow_appendsANewRow(Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();

        TableRow first = section.addRow();
        TableRow second = section.addRow();

        assertEquals(List.of(first, second), section.getRows());
    }

    @ParameterizedTest
    @MethodSource("sections")
    void addRows_appendsExistingRows(Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        TableRow row0 = new TableRow();
        TableRow row1 = new TableRow();

        section.addRows(row0, row1);

        assertEquals(List.of(row0, row1), section.getRows());
    }

    @ParameterizedTest
    @MethodSource("sections")
    void insertRow_placesItAtTheGivenPosition(
            Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        TableRow first = section.addRow();
        TableRow last = section.addRow();

        TableRow middle = section.insertRow(1);
        TableRow leading = section.insertRow(0);

        assertEquals(List.of(leading, first, middle, last), section.getRows());
    }

    @ParameterizedTest
    @MethodSource("sections")
    void replaceRow_swapsTheRowAtThePosition(
            Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        TableRow first = section.addRow();
        TableRow old = section.addRow();
        TableRow replacement = new TableRow();

        section.replaceRow(1, replacement);

        assertEquals(List.of(first, replacement), section.getRows());
        assertTrue(old.getParent().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("sections")
    void removeRows_detachesOnlyTheGivenOnes(
            Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        TableRow row0 = section.addRow();
        TableRow row1 = section.addRow();
        TableRow row2 = section.addRow();

        section.removeRows(row0, row2);

        assertEquals(List.of(row1), section.getRows());
        assertTrue(row0.getParent().isEmpty());
        assertTrue(row2.getParent().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("sections")
    void removeAllRows_leavesTheSectionEmpty(
            Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        section.addRow();
        section.addRow();

        section.removeAllRows();

        assertTrue(section.getRows().isEmpty());
    }
}
