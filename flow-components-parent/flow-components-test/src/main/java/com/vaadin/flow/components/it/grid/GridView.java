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
import com.vaadin.ui.button.Button;
import com.vaadin.ui.grid.Grid;

/**
 * @author Vaadin Ltd.
 */
public class GridView extends TestView {

    private Grid<String> grid;

    private DataProvider<String, ?> dataProvider1 = DataProvider
            .fromCallbacks(query -> IntStream
                    .range(query.getOffset(),
                            query.getOffset() + query.getLimit())
                    .mapToObj(Integer::toString), query -> 10000);

    private DataProvider<String, ?> dataProvider2 = DataProvider.ofItems("foo",
            "foob", "fooba", "foobar");

    /**
     * Creates a view with a grid.
     */
    public GridView() {
        grid = new Grid<>();

        grid.setDataProvider(dataProvider1);
        grid.addColumn(i -> i).setHeader("text");
        grid.addColumn(i -> String.valueOf(i.length()))
                .setHeader("length");

        add(grid);

        Button updateProvider = new Button("Use another provider",
                event -> setProvider(grid));
        updateProvider.setId("update-provider");

        add(updateProvider);
    }

    private void setProvider(Grid<String> grid) {
        grid.setDataProvider(grid.getDataCommunicator().getDataProvider()
                .equals(dataProvider1) ? dataProvider2 : dataProvider1);
    }
}
