/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.material;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class MaterialThemedTemplateIT extends ChromeBrowserTest {

    @Test
    public void themedUrlsAreAdded() {
        open();

        // check that all imported templates are available in the DOM
        TestBenchElement template = $("material-themed-template").first();

        TestBenchElement div = template.$("div").first();

        Assert.assertEquals("Material themed Template", div.getText());

        TestBenchElement head = $("head").first();

        List<String> hrefs = head.$("link").attribute("rel", "import").all()
                .stream().map(element -> element.getAttribute("href"))
                .collect(Collectors.toList());

        Collection<String> expectedSuffices = new LinkedList<>(Arrays.asList(
                "themed-template/theme/material/MaterialThemedTemplate.html",
                "vaadin-material-styles/color.html",
                "vaadin-material-styles/typography.html"));

        for (String href : hrefs) {
            Optional<String> matched = expectedSuffices.stream()
                    .filter(suffix -> href.endsWith(suffix)).findFirst();
            if (matched.isPresent()) {
                expectedSuffices.remove(matched.get());
            }
        }

        if (!expectedSuffices.isEmpty()) {
            Assert.fail("No imports found for the lumo specific HTML file(s) : "
                    + expectedSuffices);
        }
    }

}