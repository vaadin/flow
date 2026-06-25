/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Test;

import static com.vaadin.flow.component.html.AssertUtils.assertEquals;
import static org.junit.Assert.assertNull;

public class NativeTableCellTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void colspanMustBeNonNegative() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setColspan(-1);
    }

    @Test
    public void setColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setColspan(2);
        assertEquals("Colspan should be 2", "2",
                cell.getElement().getAttribute("colspan"));
    }

    @Test
    public void getDefaultColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        int colspan = cell.getColspan();
        assertEquals("Default colspan should be 1", 1, colspan);
    }

    @Test
    public void getColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.getElement().setAttribute("colspan", "2");
        assertEquals("Colspan should be 2", 2, cell.getColspan());
    }

    @Test
    public void resetColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.getElement().setAttribute("colspan", "2");
        cell.resetColspan();
        assertNull("Element should not have colspan attribute",
                cell.getElement().getAttribute("colspan"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rowspanMustNonNegative() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setRowspan(-1);
    }

    @Test
    public void setRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setRowspan(2);
        assertEquals("Rowspan should be 2", "2",
                cell.getElement().getAttribute("rowspan"));
    }

    @Test
    public void getDefaultRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        int rowspan = cell.getRowspan();
        assertEquals("Default rowspan should be 1", 1, rowspan);
    }

    @Test
    public void getRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.getElement().setAttribute("rowspan", "2");
        assertEquals("Rowspan should be 2", 2, cell.getRowspan());
    }

    @Test
    public void resetRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setRowspan(2);
        cell.resetRowspan();
        assertNull("Element should not have rowspan attribute",
                cell.getElement().getAttribute("rowspan"));
    }

}
