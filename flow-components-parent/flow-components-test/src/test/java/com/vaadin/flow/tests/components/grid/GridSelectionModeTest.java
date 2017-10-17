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
package com.vaadin.flow.tests.components.grid;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.grid.Grid.SelectionMode;
import com.vaadin.ui.grid.GridMultiSelectionModel;
import com.vaadin.ui.grid.GridNoneSelectionModel;
import com.vaadin.ui.grid.GridSingleSelectionModel;

public class GridSelectionModeTest {

    private Grid<String> grid;

    @Before
    public void setup() {
        grid = new Grid<>();
        grid.setItems("foo", "bar", "baz");
    }

    @Test
    public void testSelectionModes() {
        assertInstanceOf(GridSingleSelectionModel.class,
                grid.getSelectionModel().getClass());

        assertInstanceOf(GridMultiSelectionModel.class,
                grid.setSelectionMode(SelectionMode.MULTI).getClass());
        assertInstanceOf(GridMultiSelectionModel.class,
                grid.getSelectionModel().getClass());

        assertInstanceOf(GridNoneSelectionModel.class,
                grid.setSelectionMode(SelectionMode.NONE).getClass());
        assertInstanceOf(GridNoneSelectionModel.class,
                grid.getSelectionModel().getClass());

        assertInstanceOf(GridSingleSelectionModel.class,
                grid.setSelectionMode(SelectionMode.SINGLE).getClass());
        assertInstanceOf(GridSingleSelectionModel.class,
                grid.getSelectionModel().getClass());
    }

    private void assertInstanceOf(Class<?> expected, Class<?> actual) {
        Assert.assertTrue(
                actual.getName() + " should be instance of "
                        + expected.getName(),
                expected.isAssignableFrom(actual));
    }

    @Test(expected = NullPointerException.class)
    public void testNullSelectionMode() {
        grid.setSelectionMode(null);
    }

}
