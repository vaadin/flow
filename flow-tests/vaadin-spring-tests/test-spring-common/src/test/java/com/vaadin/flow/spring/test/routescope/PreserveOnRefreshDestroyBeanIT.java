/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.spring.test.AbstractSpringTest;
import com.vaadin.flow.testutil.AbstractTestBenchTest;

public class PreserveOnRefreshDestroyBeanIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/pre-destroy";
    }

    @Test
    public void routeScopedBeanIsDestroyedOnNavigationOutOfViewAfterPreserveOnRefresh() {
        open();

        navigateOut();

        waitForElementPresent(By.id("main"));

        // refresh
        getDriver().get(AbstractTestBenchTest.getTestURL(getRootURL(),
                getContextPath() + "/preserve-pre-destroy"));

        waitForElementPresent(By.id("main"));

        // navigate back
        navigateOut();

        waitForElementNotPresent(By.id("main"));

        Assert.assertTrue(isElementPresent(By.className("info")));

    }

    private void navigateOut() {
        findElement(By.id("navigate-out")).click();
    }
}
