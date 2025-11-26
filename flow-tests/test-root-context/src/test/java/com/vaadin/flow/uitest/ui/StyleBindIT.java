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

import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StyleBindIT extends ChromeBrowserTest {

    @Test
    public void toggleSignal_updatesBackgroundColor() {
        open("com.vaadin.flow.uitest.ui.StyleBindView");

        DivElement target = $(DivElement.class).id("target");
        NativeButtonElement setRed = $(NativeButtonElement.class).id("set-red");
        NativeButtonElement setGreen = $(NativeButtonElement.class)
                .id("set-green");
        NativeButtonElement setNull = $(NativeButtonElement.class)
                .id("set-null");

        // Initial red
        waitUntil(d -> "rgba(255, 0, 0, 1)"
                .equals(target.getCssValue("background-color")));

        // Change to green
        setGreen.click();
        waitUntil(d -> "rgba(0, 128, 0, 1)"
                .equals(target.getCssValue("background-color")));

        // Clear
        setNull.click();
        waitUntil(d -> !"rgba(0, 128, 0, 1)"
                .equals(target.getCssValue("background-color")));

        // Back to red
        setRed.click();
        waitUntil(d -> "rgba(255, 0, 0, 1)"
                .equals(target.getCssValue("background-color")));
    }

    @Test
    public void manualSetRemove_throwsBindingActiveException() {
        open("com.vaadin.flow.uitest.ui.StyleBindView");

        SpanElement status = $(SpanElement.class).id("status");
        NativeButtonElement manualSet = $(NativeButtonElement.class)
                .id("manual-set");
        NativeButtonElement manualRemove = $(NativeButtonElement.class)
                .id("manual-remove");

        manualSet.click();
        waitUntil(d -> "BindingActiveException".equals(status.getText()));

        manualRemove.click();
        waitUntil(d -> "BindingActiveException".equals(status.getText()));
    }

    @Test
    public void manualSetRemove_bindingRemoved_setsAndRemoves() {
        open("com.vaadin.flow.uitest.ui.StyleBindView");

        SpanElement status = $(SpanElement.class).id("status");
        NativeButtonElement manualSet = $(NativeButtonElement.class)
                .id("manual-set");
        NativeButtonElement manualRemove = $(NativeButtonElement.class)
                .id("manual-remove");
        NativeButtonElement removeBinding = $(NativeButtonElement.class)
                .id("remove-binding");

        removeBinding.click();

        manualSet.click();
        waitUntil(d -> "manual-set-ok".equals(status.getText()));

        manualRemove.click();
        waitUntil(d -> "manual-remove-ok".equals(status.getText()));
    }

    @Test
    public void detachAttach_noUpdatesWhileDetached_thenAppliesOnReattach() {
        open("com.vaadin.flow.uitest.ui.StyleBindView");

        DivElement target = $(DivElement.class).id("target");
        NativeButtonElement setGreen = $(NativeButtonElement.class)
                .id("set-green");
        NativeButtonElement detach = $(NativeButtonElement.class).id("detach");
        NativeButtonElement attach = $(NativeButtonElement.class).id("attach");

        // Ensure red initially
        waitUntil(d -> "rgba(255, 0, 0, 1)"
                .equals(target.getCssValue("background-color")));

        // Detach and change to green while detached
        detach.click();
        setGreen.click();

        // While detached, there is no element to read style from. Reattach and
        // expect latest value applied
        attach.click();
        waitUntil(d -> "rgba(0, 128, 0, 1)".equals($(DivElement.class)
                .id("target").getCssValue("background-color")));
    }
}
