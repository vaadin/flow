/**
 * Copyright (C) 2024 Vaadin Ltd
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

public class TypeScriptIT extends ChromeBrowserTest {

    private TestBenchElement template;

    protected String getTemplateTag() {
        return "type-script-view";
    }

    public void setup() throws Exception {
        super.setup();
        open();
        template = $(getTemplateTag()).first();
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
    }
}
