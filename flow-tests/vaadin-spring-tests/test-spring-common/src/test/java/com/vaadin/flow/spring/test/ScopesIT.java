/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * @author Vaadin Ltd
 *
 */
public class ScopesIT extends AbstractSpringTest {

    /**
     * The client only requests a resynchronization once it has waited for the
     * message it thinks it lost, which takes {@code maxMessageSuspendTimeout}
     * (5 seconds by default).
     */
    private static final int RESYNC_TIMEOUT_SECONDS = 30;

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

    @Test
    public void checkUiScope_afterResynchronized() throws Exception {
        getDriver().get(getTestURL() + "ui-scope");
        waitForDevServer();

        String mainId = findElement(By.id("main")).getText();

        String innerId = findElement(By.id("inner")).getText();

        Assert.assertEquals(mainId, innerId);

        // Resynchronize
        WebElement resynchronize = findElement(By.id("resynchronize"));
        resynchronize.click();

        // The client notices the skipped sync id, requests a resync and
        // rebuilds the DOM from scratch, which detaches the old elements. Wait
        // for that before interacting with the view again, so that the click
        // below is handled after the resynchronization instead of before it.
        waitUntil(ExpectedConditions.stalenessOf(resynchronize),
                RESYNC_TIMEOUT_SECONDS);

        findElement(By.id("status-check")).click();

        waitForElementPresent(By.id("ui-was-attached"));
        waitForElementPresent(By.id("ui-was-detached"));

        String anotherMainId = findElement(By.id("main")).getText();

        Assert.assertEquals(mainId, anotherMainId);

        innerId = findElement(By.id("inner")).getText();

        Assert.assertEquals(anotherMainId, innerId);
    }
}
