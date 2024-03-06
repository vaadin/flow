/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BaseHrefIT extends ChromeBrowserTest {

    @Test
    public void testBaseHref() throws URISyntaxException {
        URI baseUri = new URI(getTestURL());
        String uiUrl = baseUri.toString();
        String expectedUrl = baseUri.resolve("./link").toString();

        getDriver().get(uiUrl);
        waitForDevServer();
        Assert.assertEquals(expectedUrl, getLinkHref());

        getDriver().get(uiUrl + "/foo/bar/baz");
        Assert.assertEquals(expectedUrl, getLinkHref());
    }

    private String getLinkHref() {
        return findElement(By.tagName("a")).getAttribute("href");
    }

}
