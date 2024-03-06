/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class WebComponentIT extends ChromeBrowserTest implements HasById {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/index.html";
    }

    @Test
    public void indexPageGetsWebComponent_attributeIsReflectedToServer() {
        open();

        waitForElementVisible(By.id("show-message"));

        TestBenchElement showMessage = byId("show-message");
        waitUntil(driver -> showMessage.$("select").exists());
        TestBenchElement select = showMessage.$("select").first();

        // Selection is visibly changed and event manually dispatched
        // as else the change is not seen.
        getCommandExecutor().executeScript(
                "arguments[0].value='Peter';"
                        + "arguments[0].dispatchEvent(new Event('change'));",
                select);

        Assert.assertEquals("Selected: Peter, Parker",
                showMessage.$("span").first().getText());

        TestBenchElement noMessage = byId("no-message");

        select = noMessage.$("select").first();
        getCommandExecutor().executeScript(
                "arguments[0].value='Peter';"
                        + "arguments[0].dispatchEvent(new Event('change'));",
                select);

        Assert.assertFalse("Message should not be visible",
                noMessage.$("span").first().isDisplayed());
    }

    @Test
    public void downloadLinkHasCorrectBaseURL() {
        open();

        waitForElementVisible(By.id("show-message"));
        TestBenchElement showMessage = byId("show-message");
        waitUntil(
                driver -> showMessage.$("*").attribute("id", "link").exists());
        TestBenchElement link = showMessage.$("a").id("link");
        String href = link.getAttribute("href");
        // self check
        Assert.assertTrue(href.startsWith(getRootURL()));
        // remove host and port
        href = href.substring(getRootURL().length());
        // now the URI should starts with "/vaadin" since this is the URI of
        // embedded app
        Assert.assertThat(href,
                CoreMatchers.startsWith("/vaadin/VAADIN/dynamic/resource/"));
    }

    @Test
    public void indexPageGetsThemedWebComponent_themeIsApplied() {
        open();

        waitForElementVisible(By.tagName("themed-web-component"));

        TestBenchElement webComponent = $("themed-web-component").first();
        TestBenchElement themedComponent = webComponent.$("themed-component")
                .first();

        TestBenchElement content = themedComponent.$("div").first();
        Assert.assertNotNull("The component which should use theme doesn't "
                + "contain elements", content);

        Assert.assertEquals("rgba(255, 0, 0, 1)", content.getCssValue("color"));
    }
}
