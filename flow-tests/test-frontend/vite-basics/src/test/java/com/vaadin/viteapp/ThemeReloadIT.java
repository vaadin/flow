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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.testbench.TestBenchElement;

public class ThemeReloadIT extends ViteDevModeIT {

    @Test
    @Ignore
    public void updateStyle_changeIsReloaded() throws IOException {
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("rgba(0, 0, 255, 1)", header.getCssValue("color"));

        File baseDir = new File(System.getProperty("user.dir", "."));
        File themeFolder = new File(baseDir,
                FrontendUtils.DEFAULT_FRONTEND_DIR + "themes/vite-basics/");
        File stylesCss = new File(themeFolder, "styles.css");
        final String stylesContent = FileUtils.readFileToString(stylesCss,
                StandardCharsets.UTF_8);
        try {
            FileUtils.write(stylesCss,
                    stylesContent + "\nh2 { color: rgba(255, 0, 0, 1); }",
                    StandardCharsets.UTF_8.name());

            waitUntil(
                    webDriver -> webDriver.findElement(By.tagName("h2"))
                            .getCssValue("color").equals("rgba(255, 0, 0, 1)"),
                    30);
            header = $("h2").first();
            Assert.assertEquals("rgba(255, 0, 0, 1)",
                    header.getCssValue("color"));
        } finally {
            FileUtils.write(stylesCss, stylesContent,
                    StandardCharsets.UTF_8.name());
        }
    }
}
