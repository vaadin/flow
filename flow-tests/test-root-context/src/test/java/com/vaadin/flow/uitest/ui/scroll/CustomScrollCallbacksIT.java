/*
 * Copyright 2000-2019 Vaadin Ltd.
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

public class CustomScrollCallbacksIT extends AbstractScrollIT {
    @Test
    public void customCallbacks_customResults() throws InterruptedException {
        open();
        assertView("null");
        assertLog("");

        // Scroll to bottom
        scrollBy(0, 2000);

        int bottom = getScrollY();

        findElement(By.id("navigate")).click();

        assertView("navigated");
        assertLog("[0,0]");
        /*
         * Scroll position should not be reset, but might have changed slightly
         * because of more log rows
         */
        checkPageScroll(0, bottom, 50);

        findElement(By.id("back")).click();

        assertView("null");
        assertLog("[0,0]\n[42,-" + bottom + "]");
        /*
         * Scroll position should not be reset, but might have changed slightly
         * because of more log rows
         */
        checkPageScroll(0, bottom, 50);
    }

    private void assertView(String expected) {
        String text = findElement(By.id("view")).getText();
        Assert.assertEquals("Current view: " + expected, text);
    }

    private void assertLog(String expected) {
        String text = findElement(By.id("log")).getText();
        Assert.assertEquals(expected, text);
    }
}
