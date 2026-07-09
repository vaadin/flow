/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.contexttest.ui.PlusInRouteParameterView.TENANT_ID;

public class PlusInParameterIT extends ChromeBrowserTest {

    static final String JETTY_CONTEXT = System.getProperty(
            "vaadin.test.jettyContextPath", "/custom-context-router");

    @Override
    protected String getTestPath() {
        return JETTY_CONTEXT + "/+/plus-test";
    }

    @Test
    public void literalPlusAsFirstPathSegment_isPreserved() {
        open();
        waitForElementPresent(By.id(TENANT_ID));
        WebElement element = findElement(By.id(TENANT_ID));

        Assert.assertEquals(
                "Literal + in URL path should be preserved as route parameter.",
                "+", element.getText());
    }
}
