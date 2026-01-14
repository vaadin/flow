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
package com.vaadin.flow.uitest.ui.signal;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration tests for binding element enabled state to signals.
 */
public class BindEnabledIT extends ChromeBrowserTest {

    @Test
    public void toggleSignal_updatesEnabledState() {
        open();

        NativeButtonElement toggleChild = $(NativeButtonElement.class)
                .id("toggle-button-child");
        NativeButtonElement toggleParent = $(NativeButtonElement.class)
                .id("toggle-button-parent");
        Assert.assertEquals("", getClickInfo());

        // Initially enabled
        Assert.assertNull(getDisabledAttribute());
        getChildTarget().click();
        Assert.assertEquals("Clicked: 1", getClickInfo());

        toggleChild.click();
        // explicitly disabled
        Assert.assertEquals("true", getDisabledAttribute());

        toggleParent.click();
        // still explicitly disabled
        Assert.assertEquals("true", getDisabledAttribute());

        toggleChild.click();
        // still disabled due to parent being disabled
        Assert.assertEquals("true", getDisabledAttribute());
        // events from client to server are disabled
        getChildTarget().click();
        Assert.assertEquals("Clicked: 1", getClickInfo());

        toggleParent.click();
        // enabled again
        Assert.assertNull(getDisabledAttribute());
        getChildTarget().click();
        Assert.assertEquals("Clicked: 2", getClickInfo());
    }

    private WebElement getChildTarget() {
        return findElement(By.id("target-enabled-initially"));
    }

    private String getDisabledAttribute() {
        return getChildTarget().getDomAttribute("disabled");
    }

    private String getClickInfo() {
        return findElement(By.id("click-info")).getText();
    }
}
