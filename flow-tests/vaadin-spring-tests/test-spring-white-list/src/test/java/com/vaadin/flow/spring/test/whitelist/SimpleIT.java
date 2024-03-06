/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.whitelist;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void simplePage_withWhiteList_works() {
        TestBenchElement viewElement = $("simple-view").first();
        ButtonElement button = viewElement.$(ButtonElement.class).id("button");

        button.click();

        TextFieldElement log = viewElement.$(TextFieldElement.class).id("log");
        Assert.assertEquals(SimpleView.CLICKED_MESSAGE, log.getValue());
    }

    @Override
    protected String getTestPath() {
        return "/";
    }
}
