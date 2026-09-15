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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.TableElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AuraTableStylesIT extends ChromeBrowserTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    @Test
    public void stylesheetIsServedAndAppliedToANativeTable() {
        TableElement table = $(TableElement.class).id("default-table");

        Assert.assertEquals("separate", table.getCssValue("border-collapse"));
        Assert.assertNotEquals("0px",
                table.getCssValue("border-top-left-radius"));
        WebElement header = headerCell("default-table");
        Assert.assertEquals("8px", header.getCssValue("padding-top"));
        Assert.assertEquals("12px", header.getCssValue("padding-left"));
    }

    @Test
    public void headerCellsAlignWithTheColumnInsteadOfBeingCentered() {
        // The browser's own stylesheet centers <th>; the opt-in stylesheet has
        // to win over it without raising specificity
        Assert.assertEquals("start",
                headerCell("default-table").getCssValue("text-align"));
    }

    @Test
    public void themeVariantsChangeOnlyTheTableTheyAreOn() {
        Assert.assertEquals("1px",
                bodyCell("default-table").getCssValue("border-bottom-width"));
        Assert.assertEquals("0px", bodyCell("no-row-borders-table")
                .getCssValue("border-bottom-width"));

        Assert.assertEquals("0px",
                bodyCell("default-table").getCssValue("border-right-width"));
        Assert.assertEquals("1px", bodyCell("column-borders-table")
                .getCssValue("border-right-width"));

        Assert.assertEquals("4px",
                bodyCell("compact-table").getCssValue("padding-top"));
        Assert.assertEquals("8px",
                bodyCell("compact-table").getCssValue("padding-left"));
    }

    private WebElement headerCell(String tableId) {
        return $(TableElement.class).id(tableId).getHeaderRows().get(0)
                .getHeaderCells().get(0);
    }

    private WebElement bodyCell(String tableId) {
        return $(TableElement.class).id(tableId).getBodyRows().get(0)
                .getDataCells().get(0);
    }
}
