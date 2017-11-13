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
package com.vaadin.flow.components.it.grid;

import java.util.stream.IntStream;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.grid.Grid.SelectionMode;
import com.vaadin.ui.html.Div;

/**
 * Test view for grid's multi selection column.
 */
public class GridMultiSelectionColumnView extends TestView {

    public static final int ITEM_COUNT = 1000;

    private Div message;

    /**
     * Constructor.
     */
    public GridMultiSelectionColumnView() {
        message = new Div();
        message.setId("selected-item-count");

        Grid<String> lazyGrid = new Grid<>();
        lazyGrid.setDataProvider(DataProvider.fromCallbacks(query -> {
            return IntStream
                    .range(query.getOffset(),
                            query.getOffset() + query.getLimit())
                    .mapToObj(Integer::toString);
        }, query -> ITEM_COUNT));
        setUp(lazyGrid);
        lazyGrid.setId("lazy-grid");

        Grid<String> grid = new Grid<>();
        grid.setItems(
                IntStream.range(0, ITEM_COUNT).mapToObj(Integer::toString));
        setUp(grid);
        grid.setId("in-memory-grid");

        add(lazyGrid, grid, message);
    }

    private void setUp(Grid<String> grid) {
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.addColumn(i -> i).setHeaderLabel("text");
        grid.addColumn(i -> String.valueOf(i.length()))
                .setHeaderLabel("length");
        grid.addSelectionListener(event -> message.setText(
                "Selected item count: " + event.getAllSelectedItems().size()));
    }
}
