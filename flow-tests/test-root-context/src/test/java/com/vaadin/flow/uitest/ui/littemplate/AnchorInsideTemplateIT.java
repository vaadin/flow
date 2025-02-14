/*
 * Copyright 2000-2025 Vaadin Ltd.
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
