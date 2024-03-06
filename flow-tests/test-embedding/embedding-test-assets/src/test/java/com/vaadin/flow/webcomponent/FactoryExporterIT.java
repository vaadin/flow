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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class FactoryExporterIT extends ChromeBrowserTest implements HasById {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/interfaceBased.html";
    }

    @Test
    public void webComponentExportedByExportsWebComponentInterfaceShouldBeDisplayedAndUpgraded() {
        open();

        waitForElementVisible(By.id("interface"));
        WebElement paragraph = byId("interface", "paragraph");

        Assert.assertNotNull("Web component exported by interface based "
                + "exporter should have been upgraded", paragraph);
    }
}
