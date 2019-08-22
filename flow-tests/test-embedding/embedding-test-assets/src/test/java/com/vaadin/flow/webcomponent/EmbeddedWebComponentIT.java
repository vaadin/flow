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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class EmbeddedWebComponentIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        // no page context constant, since this is served from a custom
        // servlet within the deployed war
        return "/items/1/edit";
    }

    @Test
    public void servletPageGetsWebComponent_attributeIsReflectedToServer() {
        open();

        TestBenchElement webComponent = $("client-select").first();

        Assert.assertTrue(
                webComponent.getText().trim().startsWith("Web Component"));

        // Selection is visibly changed and event manually dispatched
        // as else the change is not seen.
        getCommandExecutor().executeScript(
                "arguments[0].value='Peter';"
                        + "arguments[0].dispatchEvent(new Event('change'));",
                webComponent.$("select").first());

        TestBenchElement msg = webComponent.$("span").first();

        Assert.assertEquals("Selected: Peter, Parker", msg.getText());

        // Check that there is correctly imported custom element
        TestBenchElement dependencyElement = $("dep-element").first();
        TestBenchElement mainDepElement = dependencyElement
                .$(TestBenchElement.class).id("main");
        Assert.assertEquals("Imported element", mainDepElement.getText());
    }
}
