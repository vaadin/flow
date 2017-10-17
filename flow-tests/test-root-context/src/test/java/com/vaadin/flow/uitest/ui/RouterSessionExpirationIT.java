/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

public class RouterSessionExpirationIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/new-router-session/";
    }

    @Test
    public void navigationAfterSessionExpired() {
        open("/new-router-session/NormalView");

        navigateToAnotherView();
        String sessionId = getSessionId();
        navigateToFirstView();
        Assert.assertEquals(sessionId, getSessionId());
        navigateToSesssionExpireView();
        Assert.assertEquals("No session", getSessionId());
        navigateToFirstView();
        Assert.assertNotEquals(sessionId, getSessionId());
    }

    @Test
    public void navigationAfterInternalError() {
        open("/new-router-session/NormalView");

        navigateToAnotherView();
        String sessionId = getSessionId();
        navigateToInternalErrorView();
        navigateToFirstView();
        Assert.assertEquals(sessionId, getSessionId());
    }

    private String getSessionId() {
        return findElement(By.id("sessionId")).getText();
    }

    private void navigateToFirstView() {
        navigateTo("NormalView");
    }

    private void navigateToAnotherView() {
        navigateTo("AnotherNormalView");
    }

    private void navigateToSesssionExpireView() {
        navigateTo("ViewWhichInvalidatesSession");
    }

    private void navigateToInternalErrorView() {
        findElement(By.linkText("ViewWhichCausesInternalException")).click();
        // Won't actually reach the view..
    }

    private void navigateTo(String linkText) {
        findElement(By.linkText(linkText)).click();
        Assert.assertNotNull(
                findElement(By.xpath("//strong[text()='" + linkText + "']")));

    }
}
