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

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class AllowInertDomEventIT extends ChromeBrowserTest {

    @Test
    public void modalDialogOpened_clickAllowInertButton_eventShouldBeReceived() {
        open();

        // Click the allow-inert button before opening modal - should work
        $(TestBenchElement.class).id(AllowInertDomEventView.ALLOW_INERT_BUTTON)
                .click();
        Assert.assertEquals("1", getAllowInertEventCount());

        // Open modal dialog
        $(NativeButtonElement.class)
                .id(AllowInertDomEventView.OPEN_MODAL_BUTTON).click();

        // Click the allow-inert button while modal is open - should still work
        $(TestBenchElement.class).id(AllowInertDomEventView.ALLOW_INERT_BUTTON)
                .click();
        Assert.assertEquals("2", getAllowInertEventCount());

        // Click again to verify events continue to work
        $(TestBenchElement.class).id(AllowInertDomEventView.ALLOW_INERT_BUTTON)
                .click();
        Assert.assertEquals("3", getAllowInertEventCount());
    }

    @Test
    public void modalDialogOpened_clickRegularButton_eventShouldNotBeReceived() {
        open();

        // Click the regular button before opening modal - should work
        $(NativeButtonElement.class).id(AllowInertDomEventView.REGULAR_BUTTON)
                .click();
        Assert.assertEquals("1", getRegularEventCount());

        // Open modal dialog
        $(NativeButtonElement.class)
                .id(AllowInertDomEventView.OPEN_MODAL_BUTTON).click();

        // Click the regular button while modal is open - should NOT work
        $(NativeButtonElement.class).id(AllowInertDomEventView.REGULAR_BUTTON)
                .click();
        Assert.assertEquals("1", getRegularEventCount());

        // Click again to verify events are still blocked
        $(NativeButtonElement.class).id(AllowInertDomEventView.REGULAR_BUTTON)
                .click();
        Assert.assertEquals("1", getRegularEventCount());
    }

    private String getAllowInertEventCount() {
        return $(SpanElement.class)
                .id(AllowInertDomEventView.ALLOW_INERT_EVENT_COUNT).getText();
    }

    private String getRegularEventCount() {
        return $(SpanElement.class)
                .id(AllowInertDomEventView.REGULAR_EVENT_COUNT).getText();
    }
}
