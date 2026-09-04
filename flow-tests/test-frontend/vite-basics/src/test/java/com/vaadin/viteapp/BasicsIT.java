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
package com.vaadin.viteapp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.FrontendUtils;
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
                hasTypescriptErrorOverlay());
    }

    @Test
    public void typescriptErrorInProjectFile_errorOverlayIsShown()
            throws IOException {
        // The checker is a separate tsc process which can fail to start
        // without failing the build, in which case type errors are never
        // reported at all and noTypescriptErrors passes for the wrong
        // reason. Adding a real error must surface an overlay.
        File typeErrorFile = new File(
                new File(System.getProperty("user.dir", ".")),
                FrontendUtils.DEFAULT_FRONTEND_DIR + "typeerror.ts");
        try {
            FileUtils.write(typeErrorFile,
                    "export const notANumber: number = 'string';\n",
                    StandardCharsets.UTF_8);
            waitUntil(driver -> hasTypescriptErrorOverlay(), 60);
        } finally {
            FileUtils.deleteQuietly(typeErrorFile);
            // Leave the checker without errors for the other tests
            waitUntil(driver -> !hasTypescriptErrorOverlay(), 60);
        }
    }

    private boolean hasTypescriptErrorOverlay() {
        return $("vite-plugin-checker-error-overlay").all().stream()
                .anyMatch(overlay -> overlay.$("main").exists());
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
