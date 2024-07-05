/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.material;

import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class NotThemedTemplateIT extends ChromeBrowserTest {

    @Test
    public void no_anyThemedUrls() {
        open();

        // check that all imported templates are available in the DOM
        TestBenchElement template = $("not-themed-template").first();

        TestBenchElement div = template.$("div").first();

        Assert.assertEquals("Template", div.getText());

        TestBenchElement head = $("head").first();

        List<String> hrefs = head.$("link").attribute("rel", "import").all()
                .stream().map(element -> element.getAttribute("href"))
                .collect(Collectors.toList());

        for (String href : hrefs) {
            Assert.assertThat(href,
                    CoreMatchers.not(CoreMatchers.containsString("material")));
            Assert.assertThat(href,
                    CoreMatchers.not(CoreMatchers.containsString("lumo")));
        }
    }

}
