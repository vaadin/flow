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
package com.vaadin.flow.uitest.ui.signal;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration tests for binding width and height to signals.
 */
public class BindWidthHeightIT extends ChromeBrowserTest {

    @Test
    public void bindWidthAndHeight_changeSignalValues() {
        open();

        NativeButtonElement setWidth100Pct = $(NativeButtonElement.class)
                .id("width-100-pct");
        NativeButtonElement setHeight100Pct = $(NativeButtonElement.class)
                .id("height-100-pct");

        NativeButtonElement setWidthNull = $(NativeButtonElement.class)
                .id("width-null");
        NativeButtonElement setHeightNull = $(NativeButtonElement.class)
                .id("height-null");

        // Initially 300px x 300px span box inside a 500px x 500px container.
        WebElement target = getTarget();
        Assert.assertEquals("300px", target.getCssValue("width"));
        Assert.assertEquals("300px", target.getCssValue("height"));
        Assert.assertNull(
                target.getDomAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assert.assertNull(
                target.getDomAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));

        setWidth100Pct.click();
        setHeight100Pct.click();
        Assert.assertEquals("500px", target.getCssValue("width"));
        Assert.assertEquals("500px", target.getCssValue("height"));
        Assert.assertNotNull(
                target.getDomAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assert.assertNotNull(
                target.getDomAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));

        setWidthNull.click();
        setHeightNull.click();
        Assert.assertTrue(Unit.getSize(target.getCssValue("width")) < 20);
        Assert.assertTrue(Unit.getSize(target.getCssValue("height")) < 20);
        Assert.assertNull(
                target.getDomAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assert.assertNull(
                target.getDomAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    private WebElement getTarget() {
        return findElement(By.id("target"));
    }
}
