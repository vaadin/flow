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
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ProdModeConfigIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/prod-mode-config-test";
    }

    @Test
    public void prodMode_liveReload_alwaysDisabled_shouldNotRenderIndicator() {
        open();

        Assert.assertEquals("productionMode is expected to be true", "true",
                findElement(By.id("productionMode")).getText());
        Assert.assertEquals(
                "devModeLiveReloadEnabled is supposed to be false"
                        + " by default in production mode",
                "false",
                findElement(By.id("devModeLiveReloadEnabled")).getText());

        Assert.assertTrue(
                findElements(By.id("vaadin-live-reload-indicator")).isEmpty());
    }

    @Test
    public void prodMode_devTools_alwaysDisabled_shouldNotRenderDevToolsPopup() {
        open();

        Assert.assertEquals(
                "devToolsEnabled is supposed to be false "
                        + "by default in production mode",
                "false", findElement(By.id("devToolsEnabled")).getText());

        Assert.assertTrue(
                findElements(By.tagName("vaadin-dev-tools")).isEmpty());
    }
}
