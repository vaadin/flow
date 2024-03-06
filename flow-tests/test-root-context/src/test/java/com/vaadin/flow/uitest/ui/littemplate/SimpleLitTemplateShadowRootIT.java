/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class SimpleLitTemplateShadowRootIT extends ChromeBrowserTest {

    private TestBenchElement template;

    protected String getTemplateTag() {
        return "simple-lit-template-shadow-root";
    }

    protected boolean shouldHaveShadowRoot() {
        return true;
    }

    public void setup() throws Exception {
        super.setup();
        open();
        template = $(getTemplateTag()).first();
    }

    @Test
    public void shadowRoot() {
        Assert.assertEquals(shouldHaveShadowRoot(),
                (Boolean) executeScript("return !!arguments[0].shadowRoot",
                        template));
    }

    @Test
    public void idMappingWorks() {
        NativeButtonElement mappedButton = template.$(NativeButtonElement.class)
                .id("mappedButton");

        Assert.assertEquals("Server button", mappedButton.getText());
        mappedButton.click();

        DivElement label = template.$(DivElement.class).id("label");
        Assert.assertEquals("Hello from server component event listener",
                label.getText());

        DivElement sortDiv = template.$(DivElement.class).id("sort");

        Assert.assertEquals("Sort", sortDiv.getText());
    }

    @Test
    public void clientPropertyAndCallbackWorks() {
        NativeButtonElement clientButton = template.$(NativeButtonElement.class)
                .id("clientButton");

        Assert.assertEquals("Client button", clientButton.getText());
        clientButton.click();

        DivElement label = template.$(DivElement.class).id("label");
        Assert.assertEquals("Hello from ClientCallable", label.getText());
    }
}
