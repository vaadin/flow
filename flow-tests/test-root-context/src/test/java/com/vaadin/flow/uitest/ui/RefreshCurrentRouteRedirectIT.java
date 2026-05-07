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

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteLayout.LAYOUT_CREATION_COUNT_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteLayout.ROUTER_LAYOUT_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectTargetView.VIEW_ID;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectView.FORWARD_AND_REFRESH;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectView.FORWARD_AND_REFRESH_LAYOUTS;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectView.REROUTE_AND_REFRESH;
import static com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectView.REROUTE_AND_REFRESH_LAYOUTS;

public class RefreshCurrentRouteRedirectIT extends ChromeBrowserTest {

    @Test
    public void refreshCurrentRouteAndLayouts_withForward_recreatesTargetAndLayout() {
        open();
        waitForElementPresent(By.id(FORWARD_AND_REFRESH_LAYOUTS));

        $(NativeButtonElement.class).id(FORWARD_AND_REFRESH_LAYOUTS).click();

        // Should now be on the forward target view
        waitForElementPresent(By.id(VIEW_ID));

        // Layout should be created twice: once for the refresh, once for the
        // forward. This verifies that the forceInstantiation flag is propagated
        // during forward.
        Assert.assertEquals(
                "Layout should be created twice when forwarding with refreshCurrentRoute(true)",
                "2", getString(LAYOUT_CREATION_COUNT_ID));
    }

    @Test
    public void refreshCurrentRouteViewOnly_withForward_recreatesTargetOnly() {
        open();
        waitForElementPresent(By.id(FORWARD_AND_REFRESH));

        String originalLayoutId = getString(ROUTER_LAYOUT_ID);

        $(NativeButtonElement.class).id(FORWARD_AND_REFRESH).click();

        // Should now be on the forward target view
        waitForElementPresent(By.id(VIEW_ID));
        String newLayoutId = getString(ROUTER_LAYOUT_ID);

        // Layout should be the same (only view refresh, not layouts)
        Assert.assertEquals(
                "Layout should NOT be recreated after forward with refreshCurrentRoute(false)",
                originalLayoutId, newLayoutId);
    }

    @Test
    public void refreshCurrentRouteAndLayouts_withReroute_recreatesTargetAndLayout() {
        open();
        waitForElementPresent(By.id(REROUTE_AND_REFRESH_LAYOUTS));

        $(NativeButtonElement.class).id(REROUTE_AND_REFRESH_LAYOUTS).click();

        // Should now show the rerouted target view
        waitForElementPresent(By.id(VIEW_ID));

        // Layout should be created twice: once for the refresh, once for the
        // reroute. This verifies that the recreateLayoutChain flag is
        // propagated during reroute.
        Assert.assertEquals(
                "Layout should be created twice when rerouting with refreshCurrentRoute(true)",
                "2", getString(LAYOUT_CREATION_COUNT_ID));
    }

    @Test
    public void refreshCurrentRouteViewOnly_withReroute_recreatesTargetOnly() {
        open();
        waitForElementPresent(By.id(REROUTE_AND_REFRESH));

        String originalLayoutId = getString(ROUTER_LAYOUT_ID);

        $(NativeButtonElement.class).id(REROUTE_AND_REFRESH).click();

        // Should now show the rerouted target view
        waitForElementPresent(By.id(VIEW_ID));
        String newLayoutId = getString(ROUTER_LAYOUT_ID);

        // Layout should be the same (only view refresh, not layouts)
        Assert.assertEquals(
                "Layout should NOT be recreated after reroute with refreshCurrentRoute(false)",
                originalLayoutId, newLayoutId);
    }

    private String getString(String id) {
        waitForElementPresent(By.id(id));
        return findElement(By.id(id)).getText();
    }
}
