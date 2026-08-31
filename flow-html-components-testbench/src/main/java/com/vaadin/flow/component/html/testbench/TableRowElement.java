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
package com.vaadin.flow.component.html.testbench;

import java.util.Arrays;
import java.util.List;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;tr&gt;</code> element.
 *
 * @since 25.3
 */
@Element("tr")
public class TableRowElement extends TestBenchElement {

    /**
     * Returns every cell of this row, in document order, both
     * <code>&lt;td&gt;</code> and <code>&lt;th&gt;</code>.
     *
     * @return the cells of this row.
     */
    public List<TableCellElement> getCells() {
        return cells("td", "th");
    }

    /**
     * Returns the cell at the given position among all the cells of this row.
     *
     * @param index
     *            the position of the cell.
     * @return the cell at that position.
     */
    public TableCellElement getCell(int index) {
        return getCells().get(index);
    }

    /**
     * Returns the data cells of this row, in document order.
     *
     * @return this row's <code>&lt;td&gt;</code> cells.
     */
    public List<TableDataCellElement> getDataCells() {
        return cells("td").stream()
                .map(cell -> cell.wrap(TableDataCellElement.class)).toList();
    }

    /**
     * Returns the header cells of this row, in document order.
     *
     * @return this row's <code>&lt;th&gt;</code> cells.
     */
    public List<TableHeaderCellElement> getHeaderCells() {
        return cells("th").stream()
                .map(cell -> cell.wrap(TableHeaderCellElement.class)).toList();
    }

    /**
     * The cells of this row are its direct children; a descendant query would
     * also pick up the cells of a table nested inside one of them.
     */
    private List<TableCellElement> cells(String... tagNames) {
        List<String> wanted = Arrays.asList(tagNames);
        return getChildren().stream()
                .filter(child -> wanted.stream().anyMatch(
                        tag -> tag.equalsIgnoreCase(child.getTagName())))
                .map(child -> child.wrap(TableCellElement.class)).toList();
    }
}
