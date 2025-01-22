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

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.DevToolsElement;

@NotThreadSafe
public class JavaLiveReloadIT extends AbstractLiveReloadIT {

    @Test
    public void deactivateLiveReload() {
        open();

        // given: live reload is deactivated
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();

        devTools.setLiveReload(false);

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // then: page is not reloaded
        DevToolsElement liveReload2 = $(DevToolsElement.class).waitForFirst();
        WebElement devTools2 = liveReload2.$("*")
                .withAttributeContainingWord("class", "dev-tools").first();
        Assert.assertTrue(
                devTools2.getAttribute("class").contains("dev-tools"));
    }
}
