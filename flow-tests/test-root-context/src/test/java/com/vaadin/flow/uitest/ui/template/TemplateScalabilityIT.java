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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

public class TemplateScalabilityIT extends ChromeBrowserTest {

    @Test
    public void openPage_allButtonsRenderSuccessfully() {
        open();

        waitUntil(input -> {
            TestBenchElement view = $("*").id("scalability-view");
            return view.$("*")
                    .attribute("id", TemplateScalabilityView.COMPLETED)
                    .exists();
        });

        TestBenchElement viewTemplate = $("*").id("scalability-view");
        int buttons = viewTemplate.$("template-scalability-panel").all().size();

        Assert.assertEquals("Template should have created "
                + TemplateScalabilityView.NUM_ITEMS + " panels with buttons.",
                TemplateScalabilityView.NUM_ITEMS, buttons);

        checkLogsForErrors();
    }

    @Element("template-scalability-panel")
    public class ScalabilityPanelElement extends TestBenchElement {

    }

    @Element("template-scalability-view")
    public class ScalabilityViewElement extends TestBenchElement {

    }
}
