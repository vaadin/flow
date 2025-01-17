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
package com.vaadin.flow.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PushStateScrollIT extends ChromeBrowserTest {
    @Test
    public void pushNoScroll() {
        testNoScrolling("push");
    }

    @Test
    public void replaceNoScroll() {
        testNoScrolling("replace");
    }

    private void testNoScrolling(String buttonId) {
        open();

        WebElement button = findElement(By.id(buttonId));

        scrollToElement(button);

        int scrollBeforeClick = getScrollY();

        // Sanity check
        Assert.assertNotEquals("Should be scrolled down before clicking", 0,
                scrollBeforeClick);

        button.click();

        Assert.assertEquals("Scroll position should not have changed",
                scrollBeforeClick, getScrollY());
    }
}
