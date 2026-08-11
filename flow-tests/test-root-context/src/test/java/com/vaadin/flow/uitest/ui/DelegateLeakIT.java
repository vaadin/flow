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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Validates the PR #25044 reviewer reproducer: the element-locator delegate
 * clone must fire the owner inside the overlay but must not leak the shortcut
 * to an owner outside it.
 */
public class DelegateLeakIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;

    @Before
    public void init() {
        open();
        $(NativeButtonElement.class).id(DelegateLeakView.OPEN_ID).click();
        eventLog = $(DivElement.class).id(DelegateLeakView.EVENT_LOG_ID);
    }

    @Test
    public void altSInOverlay_insideOwnerFires_outsideOwnerDoesNot() {
        final InputTextElement field = $(InputTextElement.class)
                .id(DelegateLeakView.FIELD_ID);
        field.focus();
        field.sendKeys(Keys.chord(Keys.ALT, "s"));

        // Barrier: the inside owner must fire. Both shortcuts react to the same
        // keydown in one round-trip, so once the inside entry is logged an
        // erroneous outside entry would already be present too.
        waitUntil(driver -> count(DelegateLeakView.INSIDE_FIRED) >= 1);
        Assert.assertEquals("Owner inside the overlay must fire exactly once",
                1, count(DelegateLeakView.INSIDE_FIRED));
        Assert.assertEquals(
                "Owner outside the overlay must not fire from the delegate clone",
                0, count(DelegateLeakView.OUTSIDE_FIRED));
    }

    private long count(String text) {
        return eventLog.findElements(By.tagName("div")).stream()
                .filter(e -> e.getText().contains(text)).count();
    }
}
