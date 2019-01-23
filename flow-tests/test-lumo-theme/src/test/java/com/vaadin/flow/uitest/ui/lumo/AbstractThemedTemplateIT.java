/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.lumo;

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

public abstract class AbstractThemedTemplateIT extends ChromeBrowserTest {

    @Test
    public void themedUrlsAreAdded() {
        open();

        // check that all imported templates are available in the DOM
        TestBenchElement template = $(getTagName()).first();

        TestBenchElement div = template.$("div").first();

        Assert.assertEquals("Lumo themed Template", div.getText());

        TestBenchElement head = $("head").first();

        List<String> hrefs = head.$("link").attribute("rel", "import").all()
                .stream().map(element -> element.getAttribute("href"))
                .collect(Collectors.toList());

        Collection<String> expectedSuffices = new LinkedList<>(Arrays.asList(
                getThemedTemplate(), "vaadin-lumo-styles/color.html",
                "vaadin-lumo-styles/typography.html",
                "vaadin-lumo-styles/sizing.html",
                "vaadin-lumo-styles/spacing.html",
                "vaadin-lumo-styles/style.html",
                "vaadin-lumo-styles/icons.html"));

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

    protected abstract String getTagName();

    protected abstract String getThemedTemplate();

}
