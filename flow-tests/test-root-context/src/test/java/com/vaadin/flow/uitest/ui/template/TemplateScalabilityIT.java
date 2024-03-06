/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
