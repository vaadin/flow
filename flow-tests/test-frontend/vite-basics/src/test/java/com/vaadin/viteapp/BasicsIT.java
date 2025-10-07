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
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

public class BasicsIT extends ViteDevModeIT {

    @Test
    public void applicationStarts() {
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("This place intentionally left empty",
                header.getText());
    }

    @Test
    public void noTypescriptErrors() throws Exception {
        // Ensure the file was loaded
        Assert.assertEquals("good", executeScript("return window.bad()"));
        Thread.sleep(2000); // Checking is async so it sometimes needs some time
        Assert.assertFalse("There should be no error overlay",
                $("vite-plugin-checker-error-overlay").first().$("main")
                        .exists());
    }

    @Test
    public void imageFromThemeShown() {
        TestBenchElement img = $("img").id(MainView.PLANT);
        waitUntil(driver -> {
            String heightString = (String) executeScript(
                    "return getComputedStyle(arguments[0]).height.replace('px','')",
                    img);
            float height = Float.parseFloat(heightString);
            return (height > 150);
        });
    }

    @Test
    public void canImportJson() {
        $("button").id(MainView.LOAD_AND_SHOW_JSON).click();
        Assert.assertEquals("{\"hello\":\"World\"}",
                $("*").id(MainView.JSON_CONTAINER).getText());
    }

    @Test
    public void componentCssDoesNotLeakToDocument() {
        String bodyColor = $("body").first().getCssValue("backgroundColor");
        Assert.assertTrue(
                "Body should be grey, not red as specified for the component",
                bodyColor.contains("211, 211, 211"));
    }

    @Test
    public void importFromDirectoryWorks() {
        String importResult = $("div").id("directoryImportResult").getText();
        Assert.assertEquals("Directory import ok", importResult);
    }

    @Test
    public void bootstrapTsCanBeModified() {
        Assert.assertEquals(1L, executeScript("return window.bootstrapMod"));
    }

    @Test
    public void toplevelAwaitWorks() {
        Assert.assertEquals("This is the value set in other.js",
                executeScript("return window.topLevelAwaitValue"));
    }
}
