/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

/**
 * Server-side component for the {@code <vaadin-grid-column-group>} element.
 * 
 * @author Vaadin Ltd.
 */
@HtmlImport("frontend://bower_components/vaadin-grid/vaadin-grid-column-group.html")
@Tag("vaadin-grid-column-group")
public class ColumnGroup extends AbstractColumn<ColumnGroup> {

    private final Collection<ColumnBase<?>> childColumns;

    /**
     * Constructs a new column group with the given header and grouping the
     * given columns.
     *
     * @param grid
     *            the owner of this column group
     * @param columns
     *            the columns to group
     */
    public ColumnGroup(Grid<?> grid, ColumnBase<?>... columns) {
        this(grid, Arrays.asList(columns));
    }

    /**
     * Constructs a new column group with the given header and grouping the
     * given columns.
     *
     * @param grid
     *            the owner of this column group
     * @param columns
     *            the columns to group
     */
    public ColumnGroup(Grid<?> grid, Collection<ColumnBase<?>> columns) {
        super(grid);
        childColumns = columns;
        columns.forEach(
                column -> getElement().appendChild(column.getElement()));
    }

    /**
     * Gets the child columns of this column group.
     * 
     * @return the child columns of this column group
     */
    public List<ColumnBase<?>> getChildColumns() {
        return new ArrayList<>(childColumns);
    }

    /**
     * Gets the underlying {@code <vaadin-grid-column-group>} element.
     * <p>
     * <strong>It is highly discouraged to directly use the API exposed by the
     * returned element.</strong>
     *
     * @return the root element of this component
     */
    @Override
    public Element getElement() {
        return super.getElement();
    }
}
