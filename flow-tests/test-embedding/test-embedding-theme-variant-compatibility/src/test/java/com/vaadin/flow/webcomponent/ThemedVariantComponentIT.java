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
package com.vaadin.flow.webcomponent;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ThemedVariantComponentIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void servletPageGetsWebComponent_attributeIsReflectedToServer() {
        open();

        TestBenchElement webComponent = $("themed-variant-web-component")
                .first();
        Assert.assertEquals("dark", webComponent.getAttribute("theme"));

        String customStyle = $("custom-style").first()
                .getAttribute("innerHTML");
        Assert.assertThat(customStyle,
                CoreMatchers.allOf(
                        CoreMatchers.containsString("theme~=\"dark\""),
                        CoreMatchers.containsString("--lumo-base-color")));
    }
}
