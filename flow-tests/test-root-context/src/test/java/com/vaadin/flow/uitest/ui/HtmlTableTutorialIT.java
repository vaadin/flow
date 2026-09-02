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
import org.openqa.selenium.NoSuchElementException;

import com.vaadin.flow.component.html.testbench.TableCaptionElement;
import com.vaadin.flow.component.html.testbench.TableColumnElement;
import com.vaadin.flow.component.html.testbench.TableColumnGroupElement;
import com.vaadin.flow.component.html.testbench.TableElement;
import com.vaadin.flow.component.html.testbench.TableHeaderCellElement;
import com.vaadin.flow.component.html.testbench.TableRowElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class HtmlTableTutorialIT extends ChromeBrowserTest {

    private static final String BACKGROUND = "rgba(151, 219, 154, 1)";
    private static final String BACKGROUND_BORDER = "rgba(220, 196, 142, 1)";
    private static final String TRANSPARENT = "rgba(0, 0, 0, 0)";

    private TableElement timetable;

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
        timetable = $(TableElement.class).id("school-timetable");
    }

    @Test
    public void basicTableRendersRowsAndCells() {
        TableElement table = $(TableElement.class).id("basic-table");
        List<TableRowElement> rows = table.getAllRows();

        Assert.assertEquals(2, rows.size());
        Assert.assertEquals(4, rows.get(0).getDataCells().size());
        Assert.assertEquals("Hi, I'm your first cell.",
                rows.get(0).getDataCells().get(0).getText());
        Assert.assertEquals("Second row, first cell.",
                rows.get(1).getDataCells().get(0).getText());
    }

    @Test
    public void rowsAddedWithAddRowLandInTheImplicitBody() {
        TableElement table = $(TableElement.class).id("basic-table");

        Assert.assertEquals("tbody",
                table.getPropertyElement("firstElementChild").getTagName());
    }

    @Test
    public void dogsTableHasColumnAndRowScopedHeaders() {
        TableElement table = $(TableElement.class).id("dogs-table");
        List<TableRowElement> rows = table.getAllRows();

        List<TableHeaderCellElement> columnHeaders = rows.get(0)
                .getHeaderCells();
        Assert.assertEquals(4, columnHeaders.size());
        columnHeaders.forEach(header -> Assert.assertEquals("col",
                header.getDomAttribute("scope")));

        TableHeaderCellElement rowHeader = rows.get(1).getHeaderCells().get(0);
        Assert.assertEquals("row", rowHeader.getDomAttribute("scope"));
        Assert.assertEquals("Breed", rowHeader.getText());
    }

    @Test
    public void animalsTableSpansColumnsAndRowsOnHeaderCells() {
        TableElement table = $(TableElement.class).id("animals-table");
        List<TableRowElement> rows = table.getAllRows();

        TableHeaderCellElement animals = rows.get(0).getHeaderCells().get(0);
        Assert.assertEquals("Animals", animals.getText());
        Assert.assertEquals("2", animals.getDomAttribute("colspan"));

        TableHeaderCellElement horse = rows.get(2).getHeaderCells().get(0);
        Assert.assertEquals("Horse", horse.getText());
        Assert.assertEquals("2", horse.getDomAttribute("rowspan"));
        Assert.assertEquals("row", horse.getDomAttribute("scope"));

        // The <th rowspan=2> means the following row holds only its own cell.
        Assert.assertEquals(1, rows.get(3).getDataCells().size());

        // getCell counts the cells a row writes, so row 3 starts at "Stallion"
        Assert.assertEquals("Stallion", table.getCell(3, 0).getText());
        // getCellCovering resolves the span, so the same slot is the <th>
        Assert.assertEquals("Horse", table.getCellCovering(3, 0).getText());
        Assert.assertEquals("Stallion", table.getCellCovering(3, 1).getText());
        // and a colspan fills both slots of its row with the one cell
        Assert.assertEquals("Animals", table.getCellCovering(0, 0).getText());
        Assert.assertEquals("Animals", table.getCellCovering(0, 1).getText());

        // the spans make this two columns wide, though no row writes two
        // header cells
        Assert.assertEquals(2, table.getColumnCount());

        Assert.assertThrows(NoSuchElementException.class,
                () -> table.getCellCovering(rows.size(), 0));
        Assert.assertThrows(NoSuchElementException.class,
                () -> table.getCellCovering(0, table.getColumnCount()));
    }

    @Test
    public void planetsTableHasCaptionAndHeadWithRowgroupSpans() {
        TableElement table = $(TableElement.class).id("planets-table");

        TableCaptionElement caption = table.getCaption();
        Assert.assertEquals("caption",
                table.getPropertyElement("firstElementChild").getTagName());
        Assert.assertTrue(caption.getText()
                .startsWith("Data about the planets of our solar system"));
        // The caption holds real markup, not just text.
        Assert.assertEquals("Nasa's Planetary Fact Sheet - Metric",
                caption.$("a").first().getText());

        Assert.assertEquals(10,
                table.getHeaderRows().get(0).getHeaderCells().size());
        // the example builds a thead and a tbody but no tfoot, so asking for
        // the missing one fails rather than handing back something empty
        Assert.assertTrue(table.hasHead());
        Assert.assertNotNull(table.getHead());
        Assert.assertFalse(table.hasFoot());
        Assert.assertTrue(table.getFooterRows().isEmpty());
        Assert.assertThrows(NoSuchElementException.class, table::getFoot);
        // one <tbody>, reachable on its own and holding all the body rows
        Assert.assertEquals(1, table.getBodies().size());
        Assert.assertEquals(table.getBodyRows().size(),
                table.getBodies().get(0).getRows().size());
        Assert.assertEquals(
                table.getHeaderRows().size() + table.getBodyRows().size(),
                table.getAllRows().size());

        TableHeaderCellElement terrestrial = table.getBodyRows().get(0)
                .getHeaderCells().get(0);
        Assert.assertEquals("Terrestrial planets", terrestrial.getText());
        Assert.assertEquals("rowgroup", terrestrial.getDomAttribute("scope"));
        Assert.assertEquals("4", terrestrial.getDomAttribute("rowspan"));
        Assert.assertEquals("2", terrestrial.getDomAttribute("colspan"));

        // The grid counts the thead row first, so the four body rows the
        // rowgroup header spans are grid rows 1 to 4, over both its columns:
        // check the two far corners of what it covers.
        Assert.assertEquals(12, table.getColumnCount());
        Assert.assertEquals("Terrestrial planets",
                table.getCellCovering(1, 0).getText());
        Assert.assertEquals("Terrestrial planets",
                table.getCellCovering(4, 1).getText());
        // the row after the span belongs to the next rowgroup header
        Assert.assertEquals("Jovian planets",
                table.getCellCovering(5, 0).getText());
        // and the cell written after it in its own row is untouched
        Assert.assertEquals("Mercury", table.getCellCovering(1, 2).getText());
        // the spans must not invent grid rows: the thead row plus the body
        // rows is all there is, so one past the last is out of range
        Assert.assertThrows(NoSuchElementException.class,
                () -> table.getCellCovering(table.getAllRows().size(), 0));
    }

    @Test
    public void colgroupRendered_withColumnsInDocumentOrder() {
        List<TableColumnGroupElement> groups = timetable.getColumnGroups();
        Assert.assertEquals(1, groups.size());

        List<TableColumnElement> columns = groups.get(0).getColumns();
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
        List<TableColumnElement> columns = columns();

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
        List<TableColumnElement> columns = columns();

        Assert.assertEquals(TRANSPARENT,
                columns.get(0).getCssValue("background-color"));
        Assert.assertEquals(BACKGROUND,
                columns.get(1).getCssValue("background-color"));
        Assert.assertEquals(BACKGROUND_BORDER,
                columns.get(4).getCssValue("background-color"));
    }

    @Test
    public void tableBodyHasOneRowPerPeriodPlusTheHeaderRow() {
        Assert.assertEquals(5, timetable.getAllRows().size());
        Assert.assertEquals(7, firstPeriodRow().getDataCells().size());
        // The row leads with a <th> for the period, so the mixed list is one
        // longer than the data cells alone
        Assert.assertEquals(8, firstPeriodRow().getCells().size());
        Assert.assertEquals("1st period",
                firstPeriodRow().getCell(0).getText());
        Assert.assertEquals("English", timetable.getCell(1, 1).getText());
    }

    private List<TableColumnElement> columns() {
        return timetable.getColumnGroups().get(0).getColumns();
    }

    private TableRowElement firstPeriodRow() {
        // Row 0 is the Mon..Sun header row, row 1 is "1st period".
        return timetable.getRow(1);
    }
}
