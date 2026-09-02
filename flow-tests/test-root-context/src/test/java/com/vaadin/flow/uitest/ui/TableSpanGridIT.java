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
import org.openqa.selenium.NoSuchElementException;

import com.vaadin.flow.component.html.testbench.TableElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration tests for the covering grid that resolves {@code colspan} and
 * {@code rowspan}.
 */
public class TableSpanGridIT extends ChromeBrowserTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    @Test
    public void rowspanZero_coversTheRestOfItsRowGroupOnly() {
        TableElement table = $(TableElement.class).id("to-end-of-group");

        Assert.assertEquals(4, table.getAllRows().size());
        Assert.assertEquals(2, table.getColumnCount());

        // the three body rows are covered by the one header cell
        for (int row = 0; row < 3; row++) {
            Assert.assertEquals("All", table.getCellCovering(row, 0).getText());
        }
        Assert.assertEquals("a", table.getCellCovering(0, 1).getText());
        Assert.assertEquals("c", table.getCellCovering(2, 1).getText());

        // the foot is a row group of its own, so the span stops before it
        Assert.assertEquals("f1", table.getCellCovering(3, 0).getText());
        Assert.assertEquals("f2", table.getCellCovering(3, 1).getText());
    }

    @Test
    public void rowspanPastTheRowGroup_isCutOffAtItsEnd() {
        TableElement table = $(TableElement.class).id("overrunning");

        Assert.assertEquals(2, table.getAllRows().size());
        Assert.assertEquals(2, table.getColumnCount());
        Assert.assertEquals("Long", table.getCellCovering(0, 0).getText());
        Assert.assertEquals("Long", table.getCellCovering(1, 0).getText());
        Assert.assertEquals("b", table.getCellCovering(1, 1).getText());

        // the span claimed nine rows but the group holds two
        Assert.assertThrows(NoSuchElementException.class,
                () -> table.getCellCovering(2, 0));
    }

    @Test
    public void shortRow_leavesTheSlotUncovered() {
        TableElement table = $(TableElement.class).id("ragged");

        Assert.assertEquals(2, table.getColumnCount());
        Assert.assertEquals("c", table.getCellCovering(1, 0).getText());

        Assert.assertThrows(NoSuchElementException.class,
                () -> table.getCellCovering(1, 1));
    }
}
