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
package com.vaadin.flow.components.it;

import java.util.stream.IntStream;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.Grid;

public class GridView extends TestView {
    public GridView() {
        Grid<String> grid = new Grid<>();

        grid.setDataProvider(DataProvider.fromCallbacks(query -> {
            return IntStream
                    .range(query.getOffset(),
                            query.getOffset() + query.getLimit())
                    .mapToObj(Integer::toString);
        }, query -> 10000));
        grid.addColumn("text", i -> i);
        grid.addColumn("length", i -> String.valueOf(i.length()));

        add(grid);
    }
}
