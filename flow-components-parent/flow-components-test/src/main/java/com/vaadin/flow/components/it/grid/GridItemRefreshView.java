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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.grid.Grid;

public class GridItemRefreshView extends TestView {

    public static final String UPDATED_FIRST_FIELD = "updated";
    public static final int UPDATED_SECOND_FIELD = 12345;

    private static class Bean {
        private String firstField;
        private int secondField;

        public Bean(String firstField, int secondField) {
            this.firstField = firstField;
            this.secondField = secondField;
        }

        public String getFirstField() {
            return firstField;
        }

        public void setFirstField(String firstField) {
            this.firstField = firstField;
        }

        public int getSecondField() {
            return secondField;
        }

        public void setSecondField(int secondField) {
            this.secondField = secondField;
        }
    }

    public GridItemRefreshView() {
        Grid<Bean> grid = new Grid<>();
        grid.addColumn("First Field", Bean::getFirstField);
        grid.addColumn("Second Field", Bean::getSecondField);
        List<Bean> items = createItems(1000);
        grid.setItems(items);

        Button refreshFirstBtn = new Button("update and refresh first item");
        refreshFirstBtn.addClickListener(event -> {
            items.get(0).setFirstField(
                    UPDATED_FIRST_FIELD + " " + items.get(0).getFirstField());
            items.get(0).setSecondField(UPDATED_SECOND_FIELD);
            grid.getDataProvider().refreshItem(items.get(0));
        });
        Button refreshMultipleBtn = new Button("update and refresh items 5-10");
        refreshMultipleBtn.addClickListener(event -> {
            items.subList(4, 10).forEach(item -> {
                item.setFirstField(
                        UPDATED_FIRST_FIELD + " " + item.getFirstField());
                item.setSecondField(UPDATED_SECOND_FIELD);
                grid.getDataProvider().refreshItem(item);
            });
        });
        Button refreshAllBtn = new Button("refresh all through data provider");
        refreshAllBtn.addClickListener(event -> {
            items.forEach(item -> {
                item.setFirstField(
                        UPDATED_FIRST_FIELD + " " + item.getFirstField());
                item.setSecondField(UPDATED_SECOND_FIELD);
            });
            grid.getDataProvider().refreshAll();
        });

        refreshFirstBtn.setId("refresh-first");
        refreshMultipleBtn.setId("refresh-multiple");
        refreshAllBtn.setId("refresh-all");
        add(grid, refreshFirstBtn, refreshMultipleBtn, refreshAllBtn);
    }

    private List<Bean> createItems(int numberOfItems) {
        return IntStream.range(0, numberOfItems).mapToObj(
                intValue -> new Bean(String.valueOf(intValue), intValue))
                .collect(Collectors.toList());
    }
}
