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
package com.vaadin.flow.uitest.ui.signal;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.TableElement;
import com.vaadin.flow.component.html.testbench.TableRowElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration tests for binding the rows of a table body to a list signal.
 */
public class BindTableRowsIT extends ChromeBrowserTest {

    private TableElement table;

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
        table = $(TableElement.class).id("table");
    }

    @Test
    public void listSignalDrivesTheRenderedRows() {
        // the bound body starts empty, and the static head is untouched by it
        Assert.assertEquals(1, table.getHeaderRows().size());
        Assert.assertTrue(table.getBodyRows().isEmpty());

        click("add");
        click("add");
        Assert.assertEquals(List.of("Planet 1", "Planet 2"), bodyTexts());

        click("rename-first");
        Assert.assertEquals(List.of("Planet 1 renamed", "Planet 2"),
                bodyTexts());

        click("move-first-last");
        Assert.assertEquals(List.of("Planet 2", "Planet 1 renamed"),
                bodyTexts());

        click("remove-first");
        Assert.assertEquals(List.of("Planet 1 renamed"), bodyTexts());

        click("clear");
        Assert.assertTrue(table.getBodyRows().isEmpty());
        // clearing the binding leaves the head alone
        Assert.assertEquals(1, table.getHeaderRows().size());
    }

    private void click(String id) {
        $(NativeButtonElement.class).id(id).click();
    }

    private List<String> bodyTexts() {
        return table.getBodyRows().stream().map(TableRowElement::getCells)
                .map(cells -> cells.get(0).getText()).toList();
    }
}
