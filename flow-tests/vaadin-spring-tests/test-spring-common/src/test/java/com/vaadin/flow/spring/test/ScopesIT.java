/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

/**
 * @author Vaadin Ltd
 *
 */
public class ScopesIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void checkSessionScope() throws Exception {
        open();

        String prefix = "foo";
        String text = findElement(By.id("message")).getText();
        Assert.assertTrue(text.startsWith(prefix));

        String sessionId = text.substring(prefix.length());
        String uiId = findElement(By.id("ui-id")).getText();

        // open another ui
        open();

        text = findElement(By.id("message")).getText();
        Assert.assertTrue(text.startsWith(prefix));

        Assert.assertEquals(sessionId, text.substring(prefix.length()));
        // self check: it should be another UI
        Assert.assertNotEquals(uiId, findElement(By.id("ui-id")).getText());
    }

    @Test
    public void checkUiScope() throws Exception {
        getDriver().get(getTestURL() + "ui-scope");
        waitForDevServer();

        String mainId = findElement(By.id("main")).getText();

        String innerId = findElement(By.id("inner")).getText();

        Assert.assertEquals(mainId, innerId);

        // open another ui
        getDriver().get(getTestURL() + "ui-scope");

        String anotherMainId = findElement(By.id("main")).getText();

        Assert.assertNotEquals(mainId, anotherMainId);

        innerId = findElement(By.id("inner")).getText();

        Assert.assertEquals(anotherMainId, innerId);
    }
}
