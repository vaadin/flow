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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeTableCellElement;
import com.vaadin.flow.component.html.testbench.NativeTableColumnElement;
import com.vaadin.flow.component.html.testbench.NativeTableColumnGroupElement;
import com.vaadin.flow.component.html.testbench.NativeTableElement;
import com.vaadin.flow.component.html.testbench.NativeTableRowElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class HtmlTableTutorialIT extends ChromeBrowserTest {

    private static final String BACKGROUND = "rgba(151, 219, 154, 1)";
    private static final String BACKGROUND_BORDER = "rgba(220, 196, 142, 1)";
    private static final String TRANSPARENT = "rgba(0, 0, 0, 0)";

    private NativeTableElement timetable;

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
        timetable = $(NativeTableElement.class).id("school-timetable");
    }

    @Test
    public void basicTableRendersRowsAndCells() {
        NativeTableElement table = $(NativeTableElement.class)
                .id("basic-table");
        List<NativeTableRowElement> rows = table.$(NativeTableRowElement.class)
                .all();

        Assert.assertEquals(2, rows.size());
        Assert.assertEquals(4,
                rows.get(0).$(NativeTableCellElement.class).all().size());
        Assert.assertEquals("Hi, I'm your first cell.",
                rows.get(0).$(NativeTableCellElement.class).first().getText());
        Assert.assertEquals("Second row, first cell.",
                rows.get(1).$(NativeTableCellElement.class).first().getText());
    }

    @Test
    public void rowsAddedWithAddRowLandInTheImplicitBody() {
        NativeTableElement table = $(NativeTableElement.class)
                .id("basic-table");
        Assert.assertEquals("tbody",
                table.getPropertyElement("firstElementChild").getTagName());
    }

    @Test
    public void colgroupRendered_withColumnsInDocumentOrder() {
        List<NativeTableColumnGroupElement> groups = timetable
                .$(NativeTableColumnGroupElement.class).all();
        Assert.assertEquals(1, groups.size());

        List<NativeTableColumnElement> columns = groups.get(0).getColumns();
        Assert.assertEquals(6, columns.size());
        Assert.assertEquals(2, columns.get(0).getSpan());
        Assert.assertEquals(1, columns.get(1).getSpan());
        Assert.assertEquals(2, columns.get(5).getSpan());
    }

    @Test
    public void colgroupIsFirstChildOfTable() {
        Assert.assertEquals("colgroup",
                timetable.getPropertyElement("firstElementChild").getTagName());
    }

    @Test
    public void columnsCarryTheStylingClasses() {
        List<NativeTableColumnElement> columns = columns();

        Assert.assertNull(columns.get(0).getDomAttribute("class"));
        Assert.assertEquals("column-background",
                columns.get(1).getDomAttribute("class"));
        Assert.assertEquals("column-fixed-width",
                columns.get(2).getDomAttribute("class"));
        Assert.assertEquals("column-background",
                columns.get(3).getDomAttribute("class"));
        Assert.assertEquals("column-background-border",
                columns.get(4).getDomAttribute("class"));
        Assert.assertEquals("column-fixed-width",
                columns.get(5).getDomAttribute("class"));
    }

    @Test
    public void columnStylesResolveInTheBrowser() {
        // A <col> paints behind the cells of its column, so the styling has to
        // be read off the <col> itself rather than off the <td> elements.
        List<NativeTableColumnElement> columns = columns();

        Assert.assertEquals(TRANSPARENT,
                columns.get(0).getCssValue("background-color"));
        Assert.assertEquals(BACKGROUND,
                columns.get(1).getCssValue("background-color"));
        Assert.assertEquals(BACKGROUND_BORDER,
                columns.get(4).getCssValue("background-color"));
    }

    @Test
    public void tableBodyHasOneRowPerPeriodPlusTheHeaderRow() {
        Assert.assertEquals(5,
                timetable.$(NativeTableRowElement.class).all().size());
        Assert.assertEquals(7,
                firstPeriodRow().$(NativeTableCellElement.class).all().size());
    }

    private List<NativeTableColumnElement> columns() {
        return timetable.$(NativeTableColumnGroupElement.class).first()
                .getColumns();
    }

    private NativeTableRowElement firstPeriodRow() {
        // Row 0 is the Mon..Sun header row, row 1 is "1st period".
        return timetable.$(NativeTableRowElement.class).all().get(1);
    }
}
