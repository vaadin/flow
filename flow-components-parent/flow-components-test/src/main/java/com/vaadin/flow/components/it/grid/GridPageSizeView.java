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
import com.vaadin.data.provider.Query;
import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.html.Label;

/**
 * @author Vaadin Ltd.
 */
public class GridPageSizeView extends TestView {

    private DataProvider<String, ?> dataProvider = DataProvider.fromCallbacks(
            query -> getStream(query).mapToObj(Integer::toString),
            query -> 10000);

    private Label info;

    /**
     * Creates a view with a grid with page size of 10.
     */
    public GridPageSizeView() {
        Grid<String> grid = new Grid<>(10);

        grid.setDataProvider(dataProvider);
        grid.addColumn(i -> i).setHeader("text");
        grid.addColumn(i -> String.valueOf(i.length()))
                .setHeader("length");

        info = new Label();
        info.setId("query-info");

        add(grid, info);
    }

    private IntStream getStream(Query<String, Void> query) {
        info.setText(String.format("Query offset: %d Query limit: %d",
                query.getOffset(), query.getLimit()));
        return IntStream.range(query.getOffset(),
                query.getOffset() + query.getLimit());
    }

}
