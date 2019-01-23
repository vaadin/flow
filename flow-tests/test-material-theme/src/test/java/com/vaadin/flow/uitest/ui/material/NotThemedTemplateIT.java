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
