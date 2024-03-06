/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html.testbench;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SelectElementIT extends ChromeBrowserTest {

    private SelectElement input;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/Select");
        input = $(SelectElement.class).id("input");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void selectByText() {
        input.selectByText("Visible text 5");
        Assert.assertEquals("value5", input.getValue());
        Assert.assertEquals("Value is 'value5'", log.getText());
        input.selectByText("Visible text 1");
        Assert.assertEquals("value1", input.getValue());
        Assert.assertEquals("Value is 'value1'", log.getText());
    }

}
