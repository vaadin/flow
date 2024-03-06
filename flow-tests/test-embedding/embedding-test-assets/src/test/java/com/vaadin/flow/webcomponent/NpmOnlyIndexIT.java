/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class NpmOnlyIndexIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/index.html";
    }

    // test for #7005, #14256
    @Test
    public void globalStylesAreUnderTheWebComponent() {
        open();

        waitForElementVisible(By.tagName("themed-web-component"));

        TestBenchElement webComponent = $("themed-web-component").first();

        List<TestBenchElement> styles = webComponent.$("style").all();

        // getAttribute wouldn't work, so we are counting the elements
        Assert.assertEquals(2, styles.size());
    }
}
