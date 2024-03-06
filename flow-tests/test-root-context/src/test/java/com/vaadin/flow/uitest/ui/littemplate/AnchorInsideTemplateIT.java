/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class AnchorInsideTemplateIT extends ChromeBrowserTest {

    @Test
    public void hrefInsideAnchorInTempalteIsSet() {
        open();

        TestBenchElement template = $(TestBenchElement.class)
                .id("template-with-anchor");
        TestBenchElement anchor = template.$(TestBenchElement.class)
                .id("anchor");

        String href = anchor.getAttribute("href");
        MatcherAssert.assertThat(href,
                CoreMatchers.containsString("VAADIN/dynamic/resource"));
    }

}
