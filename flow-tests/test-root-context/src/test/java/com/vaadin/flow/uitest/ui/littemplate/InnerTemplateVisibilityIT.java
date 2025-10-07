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
package com.vaadin.flow.uitest.ui.littemplate;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class InnerTemplateVisibilityIT extends ChromeBrowserTest {

    @Test
    public void innerTemplateIsHiddenWithDisplayNone() {
        open();

        // when inner is hidden
        NativeButtonElement toggleButton = $(NativeButtonElement.class).id(
                InnerTemplateVisibilityView.TOGGLE_INNER_VISIBILITY_BUTTON_ID);
        toggleButton.click();

        // then: element is not visible, attribute 'hidden' and 'display: none'
        // set
        TestBenchElement outer = $("*")
                .id(InnerTemplateVisibilityView.OUTER_ID);
        TestBenchElement inner = outer.$("*")
                .id(InnerTemplateVisibilityView.INNER_ID);
        Assert.assertFalse("expected inner to be hidden", inner.isDisplayed());
        Assert.assertNotNull("expected attribute hidden on inner",
                inner.getAttribute("hidden"));
        Assert.assertEquals("expected 'display: none' on inner", "none",
                inner.getCssValue("display"));
    }

    @Test
    public void innerTemplateDisplayStyleRestored() {
        open();

        // when inner is hidden and unhidden
        NativeButtonElement toggleButton = $(NativeButtonElement.class).id(
                InnerTemplateVisibilityView.TOGGLE_INNER_VISIBILITY_BUTTON_ID);
        toggleButton.click();
        toggleButton.click();

        // then: element is visible, attribute and 'display: none' are no longer
        // present
        TestBenchElement outer = $("*")
                .id(InnerTemplateVisibilityView.OUTER_ID);
        TestBenchElement inner = outer.$("*")
                .id(InnerTemplateVisibilityView.INNER_ID);
        Assert.assertTrue("expected inner to be visible", inner.isDisplayed());
        Assert.assertNull("inner should not have attribute hidden",
                inner.getAttribute("hidden"));
        Assert.assertEquals("expected 'display: block' on inner", "block",
                inner.getCssValue("display"));
    }

    @Test
    public void outerTemplateIsHiddenWithAttributeOnly() {
        open();

        // when hidden
        NativeButtonElement toggleButton = $(NativeButtonElement.class).id(
                InnerTemplateVisibilityView.TOGGLE_OUTER_VISIBILITY_BUTTON_ID);
        toggleButton.click();

        // then: element is not visible, attribute 'hidden' is set but
        // 'display: none' is not set
        WebElement outer = findElement(
                By.id(InnerTemplateVisibilityView.OUTER_ID));
        Assert.assertFalse("expected outer to be hidden", outer.isDisplayed());
        Assert.assertNotNull("expected attribute hidden on outer",
                outer.getAttribute("hidden"));
        Assert.assertEquals("expected no style attribute", "",
                outer.getAttribute("style"));
    }
}
