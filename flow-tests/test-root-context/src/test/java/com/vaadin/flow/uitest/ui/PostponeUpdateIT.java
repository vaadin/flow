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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PostponeUpdateIT extends ChromeBrowserTest {

    @Test
    public void postpone_continueKeepsCurrentUrl() {
        open();

        findElement(By.id("link")).click();
        $(NativeButtonElement.class).id("proceedButton").click();

        String updatedUrl = getDriver().getCurrentUrl();

        // navigate again to see that we do not revert to url we came in with.
        findElement(By.id("link")).click();

        waitUntil(driver -> isElementPresent(By.id("proceedButton")));

        Assert.assertTrue(String.format(
                "Before proceed, the URL in the address bar should stay as %s. But, it was %s",
                updatedUrl, getDriver().getCurrentUrl()),
                getDriver().getCurrentUrl().equals(updatedUrl));
        String currentTarget = findElement(By.id("link")).getText();

        $(NativeButtonElement.class).id("proceedButton").click();

        Assert.assertFalse(
                "Proceeding should have updated parameter from "
                        + currentTarget,
                getDriver().getCurrentUrl().endsWith(currentTarget));
    }

    @Test
    public void postpone_cancelResetsUrlOnBack() {
        open();

        findElement(By.id("link")).click();
        $(NativeButtonElement.class).id("proceedButton").click();

        String updatedUrl = getDriver().getCurrentUrl();

        // Go back to previous
        getDriver().navigate().back();
        waitUntil(driver -> isElementPresent(By.id("cancelButton")));

        $(NativeButtonElement.class).id("cancelButton").click();

        Assert.assertTrue("Canceling should revert to " + updatedUrl,
                getDriver().getCurrentUrl().equals(updatedUrl));
    }

}
