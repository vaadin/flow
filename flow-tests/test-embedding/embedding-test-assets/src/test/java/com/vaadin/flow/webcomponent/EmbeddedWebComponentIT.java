/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class EmbeddedWebComponentIT extends ChromeBrowserTest
        implements HasById {

    @Override
    protected String getTestPath() {
        // no page context constant, since this is served from a custom
        // servlet within the deployed war
        return "/items/1/edit";
    }

    @Test
    public void servletPageGetsWebComponent_attributeIsReflectedToServer() {
        open();

        // Check that there is no pwa install prompt
        Assert.assertFalse(isElementPresent(By.id("pwa-ip")));

        TestBenchElement webComponent = $("client-select").first();

        // Selection is visibly changed and event manually dispatched
        // as else the change is not seen.
        getCommandExecutor().executeScript(
                "arguments[0].value='Peter';"
                        + "arguments[0].dispatchEvent(new Event('change'));",
                webComponent.$("select").first());

        TestBenchElement msg = webComponent.$("span").first();

        Assert.assertEquals("Selected: Peter, Parker", msg.getText());

        // Check that there is correctly imported custom element
        TestBenchElement dependencyElement = webComponent.$("dep-element")
                .first();
        Assert.assertEquals("Imported element",
                byId(dependencyElement, "main").getText());
    }
}
