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
package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NoAppBundleIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Test
    public void noFrontendFilesCreated() throws IOException {
        waitForElementPresent(By.id("hello-component"));

        File baseDir = new File(System.getProperty("user.dir", "."));

        // should use default prod bundle
        assertDefaultProdBundle(baseDir);
        assertProdBundlePackageNotCreated(baseDir);

        Assert.assertFalse("No node_modules should be created",
                new File(baseDir, "node_modules").exists());

        Assert.assertFalse("No package.json should be created",
                new File(baseDir, "package.json").exists());
        Assert.assertFalse("No vite generated should be created",
                new File(baseDir, "vite.generated.ts").exists());
        Assert.assertFalse("No vite config should be created",
                new File(baseDir, "vite.config.ts").exists());

        Assert.assertFalse("No types should be created",
                new File(baseDir, "types.d.ts").exists());
        Assert.assertFalse("No tsconfig should be created",
                new File(baseDir, "tsconfig.json").exists());
    }

    @Test
    public void serviceWorkerIsIncludedAndServed() {
        getDriver().get(getRootURL() + "/view/sw.js");
        String pageSource = driver.getPageSource();
        Assert.assertFalse("Service Worker is not served properly",
                pageSource.contains("Error 404 Not Found")
                        || pageSource.contains("Could not navigate to"));
    }

    private void assertDefaultProdBundle(File baseDir) throws IOException {
        File indexHtml = new File(baseDir,
                "target/classes/META-INF/VAADIN/webapp/index.html");
        Assert.assertTrue("Prod bundle should be copied", indexHtml.exists());
        String indexHtmlContent = FileUtils.readFileToString(indexHtml,
                StandardCharsets.UTF_8);
        Assert.assertTrue("Expected default production bundle to be used",
                indexHtmlContent.contains("default production bundle"));
    }

    private void assertProdBundlePackageNotCreated(File baseDir) {
        File bundlesFolder = new File(baseDir, "src/main/bundles");
        Assert.assertFalse("src/main/bundles folder should not be created",
                bundlesFolder.exists());
    }
}
