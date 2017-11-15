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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.ui.grid.Grid.Column;

public class GridColumnGroupingTest {

    Grid<String> grid;
    Column<String> firstColumn;
    Column<String> secondColumn;
    Column<String> thirdColumn;

    @Before
    public void init() {
        grid = new Grid<>();
        firstColumn = grid.addColumn(str -> str);
        secondColumn = grid.addColumn(str -> str);
        thirdColumn = grid.addColumn(str -> str);
    }

    @Test
    public void merged_column_order() {
        Assert.assertEquals(
                Arrays.asList(firstColumn, secondColumn, thirdColumn),
                grid.getParentColumns());
        ColumnGroup merged = grid.mergeColumns(firstColumn, thirdColumn);
        Assert.assertEquals(Arrays.asList(merged, secondColumn),
                grid.getParentColumns());
        ColumnGroup secondMerge = grid.mergeColumns(merged, secondColumn);
        Assert.assertEquals(Arrays.asList(secondMerge),
                grid.getParentColumns());
        Assert.assertEquals(
                Arrays.asList(firstColumn, thirdColumn, secondColumn),
                grid.getColumns());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cant_merge_columns_not_in_grid() {
        Column<String> otherColumn = new Grid<String>().addColumn(str -> str);
        grid.mergeColumns(firstColumn, otherColumn);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cant_merge_already_merged_columns() {
        grid.mergeColumns(firstColumn, secondColumn);
        grid.mergeColumns(firstColumn, thirdColumn);
    }
}
