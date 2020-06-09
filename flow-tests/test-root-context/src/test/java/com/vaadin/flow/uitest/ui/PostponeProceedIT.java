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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PostponeProceedIT extends ChromeBrowserTest {

    @Test
    public void proceedPostponedNavigationImmediately_navigationIsDone() {
        open();

        findElement(By.id("link")).click();

        waitUntil(driver -> isElementPresent(By.id("target")));
        Assert.assertTrue(String.format(
                "After proceed, the URL in the address bar should ends with %s. But, it was %s",
                PostponeProceedView.ProceedResultView.class.getSimpleName(),
                getDriver().getCurrentUrl()),
                getDriver().getCurrentUrl()
                        .endsWith(PostponeProceedView.ProceedResultView.class
                                .getSimpleName()));
    }

    @Test
    public void proceedPostponedNavigationWithRouterLink_withDelay_urlIsChangedAfterProceed() {
        open();

        String url = driver.getCurrentUrl();

        findElement(By.id("delayedProceedLink")).click();

        Assert.assertTrue(isElementPresent(By.id("delayedProceedLink")));
        Assert.assertTrue(!isElementPresent(By.id("target")));
        Assert.assertEquals(url, driver.getCurrentUrl());

        findElement(By.id("proceedButton")).click();

        waitUntil(driver -> isElementPresent(By.id("target")));
        Assert.assertTrue(String.format(
                "After proceed, the URL in the address bar should ends with %s. But, it was %s",
                PostponeProceedView.DelayedProceedTargetView.class
                        .getSimpleName(),
                getDriver().getCurrentUrl()),
                getDriver().getCurrentUrl().endsWith(
                        PostponeProceedView.DelayedProceedTargetView.class
                                .getSimpleName()));
    }

    @Test
    public void proceedPostponedNavigationWithUINavigate_withDelay_urlIsChangedAfterProceed() {
        open();

        String url = driver.getCurrentUrl();

        findElement(By.id("postponedNavigateButton")).click();

        Assert.assertTrue(isElementPresent(By.id("postponedNavigateButton")));
        Assert.assertTrue(!isElementPresent(By.id("target")));
        Assert.assertEquals(url, driver.getCurrentUrl());

        findElement(By.id("proceedButton")).click();

        waitUntil(driver -> isElementPresent(By.id("target")));
        Assert.assertTrue(String.format(
                "After proceed, the URL in the address bar should ends with %s. But, it was %s",
                PostponeProceedView.DelayedProceedTargetView.class
                        .getSimpleName(),
                getDriver().getCurrentUrl()),
                getDriver().getCurrentUrl().endsWith(
                        PostponeProceedView.DelayedProceedTargetView.class
                                .getSimpleName()));
    }
}
