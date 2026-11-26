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
import java.nio.file.Files;
import java.nio.file.Path;

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
        Path launches = Path.of(System.getProperty("user.dir", "."),
                "target/compiler-launches.jsonl");
        waitUntil(driver -> {
            try {
                String launch = Files.readString(launches);
                return launch.contains("@typescript/native/bin/tsc")
                        && launch.contains("\"typescriptVersion\":\"6.")
                        && launch.contains("\"hasCompilerApi\":true");
            } catch (IOException e) {
                return false;
            }
        }, 60);
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
            // The drawer shows the diagnostics page served by the dev server,
            // so its content also proves the page is reachable through Flow
            waitUntil(driver -> {
                String diagnostics = getCheckerDrawerText();
                return diagnostics.contains("typeerror.ts")
                        && diagnostics.contains("TS2322");
            }, 60);
            // Proves the badge lookup that the error-free checks rely on
            waitUntil(driver -> hasTypescriptErrorOverlay(), 60);
        } finally {
            FileUtils.deleteQuietly(typeErrorFile);
            // Leave the checker without errors for the other tests
            waitUntil(driver -> !hasTypescriptErrorOverlay(), 60);
        }
    }

    @Test
    public void checkerOverlay_viewportHighPage_noScrollbar() {
        waitUntil(driver -> (Boolean) executeScript(
                "return !!document.querySelector('vite-plugin-checker-overlay')"),
                60);
        Assert.assertEquals(
                "A page that fills the viewport should not overflow it",
                Boolean.TRUE, executeScript(
                        """
                                for (const child of document.body.children) {
                                    if (child.localName !== 'vite-plugin-checker-overlay') {
                                        child.style.display = 'none';
                                    }
                                }
                                document.documentElement.style.height = '100%';
                                document.body.style.margin = '0';
                                document.body.style.height = '100%';
                                const shell = document.createElement('div');
                                shell.style.height = '100%';
                                document.body.prepend(shell);
                                return document.documentElement.scrollHeight
                                    === window.innerHeight;
                                """));
    }

    @Test
    public void checkerOverlay_coveredByPositionedContent_staysOnTop() {
        waitUntil(driver -> (Boolean) executeScript("""
                return !!document.querySelector('vite-plugin-checker-overlay')
                    ?.shadowRoot?.querySelector('.badge')
                """), 60);
        Assert.assertEquals(
                "The checker overlay should stay above positioned content",
                Boolean.TRUE, executeScript(
                        """
                                const cover = document.createElement('div');
                                cover.style.cssText = 'position: fixed; inset: 0; z-index: 1000';
                                document.body.append(cover);
                                const host = document.querySelector('vite-plugin-checker-overlay');
                                // The badge only shows when there are problems
                                const target = host.shadowRoot.querySelector('.badge');
                                target.hidden = false;
                                const { x, y, width, height } = target.getBoundingClientRect();
                                return document.elementFromPoint(x + width / 2, y + height / 2)
                                    === host;
                                """));
    }

    private boolean hasTypescriptErrorOverlay() {
        // The badge label reads e.g. "Checker: 1 error" or
        // "Checker: no problems"
        return (Boolean) executeScript(
                """
                        return [...document.querySelectorAll('vite-plugin-checker-overlay')]
                            .some(overlay => /\\d+ errors?\\b/.test(
                                overlay.shadowRoot?.querySelector('.label')?.textContent ?? ''));
                        """);
    }

    private String getCheckerDrawerText() {
        return (String) executeScript(
                """
                        return [...document.querySelectorAll('vite-plugin-checker-overlay')]
                            .map(overlay => overlay.shadowRoot.querySelector('iframe'))
                            .map(frame => frame?.contentDocument?.body?.innerText ?? '')
                            .join('\\n');
                        """);
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
