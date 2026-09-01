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

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                Named.of("thead varargs",
                        rows -> new TableHead(rows.toArray(TableRow[]::new))),
                Named.of("thead list", TableHead::new),
                Named.of("tbody varargs",
                        rows -> new TableBody(rows.toArray(TableRow[]::new))),
                Named.of("tbody list", TableBody::new),
                Named.of("tfoot varargs",
                        rows -> new TableFoot(rows.toArray(TableRow[]::new))),
                Named.of("tfoot list", TableFoot::new));
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

        section.add(row0, row1);

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
    void replace_swapsTheGivenRow(Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        TableRow first = section.addRow();
        TableRow old = section.addRow();
        TableRow replacement = new TableRow();

        section.replace(old, replacement);

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

        section.remove(row0, row2);

        assertEquals(List.of(row1), section.getRows());
        assertTrue(row0.getParent().isEmpty());
        assertTrue(row2.getParent().isEmpty());
    }

    /**
     * The interface has to stay public: a method declared by a package-private
     * type is not reflectively invocable from the outside even when inherited
     * into a public class, which breaks bean introspection and any tooling
     * reflecting over the components.
     */
    @ParameterizedTest
    @MethodSource("sections")
    void rowMethods_areReflectivelyInvocable(
            Supplier<TableRowContainer> factory) throws Exception {
        TableRowContainer section = factory.get();
        section.addRow();

        Method getRows = section.getClass().getMethod("getRows");

        assertTrue(java.lang.reflect.Modifier
                .isPublic(getRows.getDeclaringClass().getModifiers()));
        assertEquals(section.getRows(),
                assertDoesNotThrow(() -> getRows.invoke(section)));
    }

    @ParameterizedTest
    @MethodSource("sections")
    void removeAllRows_leavesTheSectionEmpty(
            Supplier<TableRowContainer> factory) {
        TableRowContainer section = factory.get();
        section.addRow();
        section.addRow();

        section.removeAll();

        assertTrue(section.getRows().isEmpty());
    }
}
