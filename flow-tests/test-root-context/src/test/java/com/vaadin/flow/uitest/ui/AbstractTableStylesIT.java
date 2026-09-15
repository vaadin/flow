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

/**
 * The contract both table stylesheets have to fulfil. The subclasses name the
 * values their own theme is expected to produce; no theme is loaded in this
 * test application, so those are the fallbacks each stylesheet declares.
 */
public abstract class AbstractTableStylesIT extends ChromeBrowserTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    /** Cell padding in the block direction, e.g. {@code "8px"}. */
    protected abstract String expectedCellPaddingBlock();

    /** Cell padding in the inline direction, e.g. {@code "12px"}. */
    protected abstract String expectedCellPaddingInline();

    /** Header cell font size, {@code "16px"} when the sheet leaves it alone. */
    protected abstract String expectedHeaderFontSize();

    /** Corner radius of the table, {@code "0px"} for a square theme. */
    protected abstract String expectedBorderRadius();

    /** Cell padding of a {@code theme="compact"} table, block then inline. */
    protected abstract String[] expectedCompactCellPadding();

    @Test
    public void stylesheetIsServedAndAppliedToANativeTable() {
        TableElement table = $(TableElement.class).id("default-table");

        Assert.assertEquals("separate", table.getCssValue("border-collapse"));
        Assert.assertEquals(expectedBorderRadius(),
                table.getCssValue("border-top-left-radius"));

        WebElement header = headerCell("default-table");
        Assert.assertEquals(expectedCellPaddingBlock(),
                header.getCssValue("padding-top"));
        Assert.assertEquals(expectedCellPaddingInline(),
                header.getCssValue("padding-left"));
        Assert.assertEquals(expectedHeaderFontSize(),
                header.getCssValue("font-size"));
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

        String[] compactPadding = expectedCompactCellPadding();
        WebElement compactCell = bodyCell("compact-table");
        Assert.assertEquals(compactPadding[0],
                compactCell.getCssValue("padding-top"));
        Assert.assertEquals(compactPadding[1],
                compactCell.getCssValue("padding-left"));
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
