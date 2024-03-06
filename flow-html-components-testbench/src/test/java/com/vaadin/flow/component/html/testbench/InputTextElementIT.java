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

public class InputTextElementIT extends ChromeBrowserTest {

    private InputTextElement input;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/InputText");
        input = $(InputTextElement.class).id("input");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void getSetValue() {
        Assert.assertEquals("", input.getValue());
        input.setValue("foo");
        Assert.assertEquals("foo", input.getValue());
        Assert.assertEquals("Value is 'foo'", log.getText());
    }

    @Test
    public void setValueEmpty() {
        input.setValue("foo");
        input.setValue("");
        Assert.assertEquals("", input.getValue());
        Assert.assertEquals("Value is ''", log.getText());
    }

    @Test
    public void clearEmpty() {
        input.clear();
        Assert.assertEquals("", input.getValue());
        Assert.assertEquals("", log.getText());
    }

    @Test
    public void clearWithValue() {
        input.setValue("foobar");
        input.clear();
        Assert.assertEquals("", input.getValue());
        Assert.assertEquals("Value is ''", log.getText());
    }
}
