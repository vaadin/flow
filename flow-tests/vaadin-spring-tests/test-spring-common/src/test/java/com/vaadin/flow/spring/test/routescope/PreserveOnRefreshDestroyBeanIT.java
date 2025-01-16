/*
 * Copyright 2000-2025 Vaadin Ltd.
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
