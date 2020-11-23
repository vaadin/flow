/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreOSGi.class)
public class RedirectToPushIT extends ChromeBrowserTest {

    @Test
    public void pushIsSetAfterNavigation() {
        open();

        $(NativeButtonElement.class).first().click();

        // when running V15-Bootstrapping push must be configured in AppShell
        // otherwise @Push annotation in routes are logged in startup but
        // ignored.
        String pushMode = Boolean.getBoolean(
                "vaadin.useDeprecatedV14Bootstrapping") ? "AUTOMATIC"
                        : "DISABLED";

        Assert.assertEquals("Push mode: " + pushMode,
                $(TestBenchElement.class).id("pushMode").getText());
    }
}
