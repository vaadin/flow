/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.devtools;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DevToolsIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view-disabled-dev-tools/com.vaadin.flow.uitest.devtools.DevToolsView";
    }

    @Test
    public void devMode_devTools_disabled_shouldNotRenderDevTools() {
        open();
        Assert.assertTrue("No dev tools popup expected when it is disabled",
                findElements(By.tagName("vaadin-dev-tools")).isEmpty());

        Assert.assertTrue(
                "Live reload is expected to be disabled when dev tools are "
                        + "disabled",
                findElements(By.id("vaadin-live-reload-indicator")).isEmpty());
    }
}
