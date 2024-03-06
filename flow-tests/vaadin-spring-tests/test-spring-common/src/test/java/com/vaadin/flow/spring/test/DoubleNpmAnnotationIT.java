/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

public class DoubleNpmAnnotationIT extends AbstractSpringTest {

    @Test
    public void bothPaperWebComponentsAreLoaded() throws Exception {
        open();
        List<TestBenchElement> paperCheckboxes = $("paper-checkbox").all();
        List<TestBenchElement> paperInputs = $("paper-input").all();

        // check that elements are on the page
        Assert.assertTrue("Should have found a 'paper-checkbox'",
                paperCheckboxes.size() > 0);
        Assert.assertTrue("Should have found a 'paper-input'",
                paperInputs.size() > 0);

        // verify that the paper components are upgraded
        Assert.assertNotNull(
                "'paper-checkbox' should have had element in shadow dom",
                paperCheckboxes.get(0).$("checkboxContainer"));
        Assert.assertNotNull(
                "'paper-input' should have had element in shadow dom",
                paperInputs.get(0).$("paper-input-container"));
    }

    @Override
    protected String getTestPath() {
        return "/double-npm-annotation";
    }
}
