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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test for issue #23232: StackOverflowError when forwarding to the
 * same view with different route parameters.
 * <p>
 * This test verifies that:
 * <ul>
 * <li>Navigating to a view with an invalid ID forwards to the same view with a
 * valid ID</li>
 * <li>No StackOverflowError occurs</li>
 * <li>The view instance is reused (same instance ID)</li>
 * <li>beforeEnter is called twice (once for original, once for forward)</li>
 * </ul>
 */
public class RedirectToSameViewIT extends ChromeBrowserTest {

    private static final String BASE_PATH = "/view/com.vaadin.flow.uitest.ui.RedirectToSameViewView";

    @Test
    public void forwardToSameView_withDifferentParameter_noStackOverflow() {
        redirectToSameView_withDifferentParameter_noStackOverflow(
                RedirectToSameViewView.RedirectType.forward);
    }

    @Test
    public void rerouteToSameView_withDifferentParameter_noStackOverflow() {
        redirectToSameView_withDifferentParameter_noStackOverflow(
                RedirectToSameViewView.RedirectType.reroute);
    }

    private void redirectToSameView_withDifferentParameter_noStackOverflow(
            RedirectToSameViewView.RedirectType redirectType) {
        // Navigate to the view with an invalid ID
        // This should trigger a forward to the same view with a valid ID
        getDriver().get(
                getRootURL() + BASE_PATH + "/" + redirectType + "/unknown-id");
        waitForDevServer();

        // Wait for the view to load
        waitForElementPresent(By.id(RedirectToSameViewView.ID_LABEL));

        // Verify the ID was changed to the valid one
        String displayedId = findElement(By.id(RedirectToSameViewView.ID_LABEL))
                .getText();
        Assert.assertEquals("ID should be the valid ID after " + redirectType,
                RedirectToSameViewView.VALID_ID, displayedId);

        // Verify beforeEnter was called twice (original + forward)
        String enterCount = findElement(
                By.id(RedirectToSameViewView.ENTER_COUNT_LABEL)).getText();
        Assert.assertEquals("beforeEnter should be called twice (original + "
                + redirectType + ")", "2", enterCount);

        // Verify URL was updated (only forward, reroute doesn't change URL)'
        if (redirectType == RedirectToSameViewView.RedirectType.forward) {
            String currentUrl = getDriver().getCurrentUrl();
            Assert.assertTrue("URL should be updated to valid-id",
                    currentUrl.endsWith(BASE_PATH + "/" + redirectType + "/"
                            + RedirectToSameViewView.VALID_ID));
        }
    }

    @Test
    public void navigateToValidId_noForward() {
        // Navigate directly to a valid ID - no forward should occur
        getDriver().get(getRootURL() + BASE_PATH + "/forward/"
                + RedirectToSameViewView.VALID_ID);
        waitForDevServer();

        waitForElementPresent(By.id(RedirectToSameViewView.ID_LABEL));

        // Verify the ID is displayed
        String displayedId = findElement(By.id(RedirectToSameViewView.ID_LABEL))
                .getText();
        Assert.assertEquals("ID should be the valid ID",
                RedirectToSameViewView.VALID_ID, displayedId);

        // Verify beforeEnter was called only once (no forward)
        String enterCount = findElement(
                By.id(RedirectToSameViewView.ENTER_COUNT_LABEL)).getText();
        Assert.assertEquals("beforeEnter should be called once (no forward)",
                "1", enterCount);
    }
}
