/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.TestBenchElement;

public class DevToolsPluginIT extends ChromeBrowserTest {

    @Test
    public void devToolsPluginWorks() {
        open();
        DevToolsElement devTools = $(DevToolsElement.class).first();
        devTools.expand();
        devTools.showTab("Hello");
        TestBenchElement myTool = devTools.$("my-tool").first();

        assertMessages(myTool, "plugin-init", "activate called");
        myTool.$(NativeButtonElement.class).first().click();
        assertMessages(myTool, "plugin-init", "activate called",
                "Response for Hello from dev tools plugin");

        $(NativeButtonElement.class).id("refresh").click();
        Assert.assertEquals("Hello from dev tools plugin",
                $("div").id("injected").getText());

        devTools.showTab("code");
        devTools.showTab("Hello");
        assertMessages(myTool, "plugin-init", "activate called",
                "Response for Hello from dev tools plugin", "deactivate called",
                "activate called");

    }

    private void assertMessages(TestBenchElement myTool, String... expected) {
        waitUntil(driver -> {
            List<TestBenchElement> rows = myTool.$("*")
                    .attribute("class", "plugin-log").all();
            String[] actual = rows.stream().map(row -> row.getText())
                    .toArray(String[]::new);
            if (Arrays.deepEquals(expected, actual)) {
                return true;
            }
            return false;
        });
    }
}
