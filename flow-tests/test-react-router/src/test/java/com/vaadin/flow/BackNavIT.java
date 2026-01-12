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
package com.vaadin.flow;

import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class BackNavIT extends ChromeBrowserTest {

    public static final String BACK_NAV_FIRST_VIEW = "/view/com.vaadin.flow.BackNavFirstView";
    public static final String BACK_NAV_SECOND_VIEW = "/view/com.vaadin.flow.BackNavSecondView?param";

    // Test for https://github.com/vaadin/flow/issues/19839
    @Test
    public void serverSideNavigation_testBackButtonAfterHistoryStateChange() {
        navigateAndPressBack(() -> $(NativeButtonElement.class).first());
    }

    // Test for https://github.com/vaadin/flow/issues/21243
    @Test
    public void routerLinkNavigation_testBackButtonAfterHistoryStateChange() {
        navigateAndPressBack(() -> $(AnchorElement.class).first());
    }

    private void navigateAndPressBack(
            Supplier<TestBenchElement> navigationElement) {
        getDriver().get(getTestURL(getRootURL(), BACK_NAV_FIRST_VIEW, null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        navigationElement.get().click();

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_SECOND_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_SECOND_VIEW);
        }

        // Navigate back; ensure we get the first URL again
        getDriver().navigate().back();
        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        Assert.assertTrue("Expected button is not available.",
                navigationElement.get().isDisplayed());
    }

    @Test
    public void backButtonWorksAndContentUpdatesAfterPageRefresh() {
        getDriver().get(getTestURL(getRootURL(), BACK_NAV_FIRST_VIEW, null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        $(NativeButtonElement.class).first().click();

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_SECOND_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_SECOND_VIEW);
        }
        // Refresh page
        getDriver().navigate().refresh();

        waitUntil(driver -> $(SpanElement.class).id(BackNavSecondView.CALLS)
                .isDisplayed());

        // Navigate back; ensure we get the first URL again
        getDriver().navigate().back();

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        Assert.assertTrue("Expected button is not available.",
                $(NativeButtonElement.class).first().isDisplayed());
    }

    @Test
    public void validateNoAfterNavigationForReplaceState() {
        getDriver().get(getTestURL(getRootURL(), BACK_NAV_FIRST_VIEW, null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        $(NativeButtonElement.class).first().click();

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_SECOND_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_SECOND_VIEW);
        }

        Assert.assertEquals("Second view: 1",
                $(SpanElement.class).id(BackNavSecondView.CALLS).getText());
    }

}
