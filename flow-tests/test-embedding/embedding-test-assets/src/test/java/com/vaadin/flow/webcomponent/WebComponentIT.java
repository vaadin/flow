/*
 * Copyright 2000-2020 Vaadin Ltd.
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
