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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AddOnIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.addon.AddOnView";
    }

    @Test
    public void selectElementExists() {
        final WebElement select = findElement(By.cssSelector("select"));
        Assert.assertTrue("'select' element not visible", select.isDisplayed());
    }

    @Test
    public void noFrontendFilesCreated() {
        File baseDir = new File(System.getProperty("user.dir", "."));

        // shouldn't create a dev-bundle
        Assert.assertFalse("No dev-bundle should be created",
                new File(baseDir, "target/" + Constants.DEV_BUNDLE_LOCATION)
                        .exists());
        Assert.assertFalse("No node_modules should be created",
                new File(baseDir, "node_modules").exists());

        Assert.assertFalse("No package.json should be created",
                new File(baseDir, "package.json").exists());
        Assert.assertFalse("No package-lock.json should be created",
                new File(baseDir, "package-lock.json").exists());
        Assert.assertFalse("No vite generated should be created",
                new File(baseDir, "vite.generated.ts").exists());
        Assert.assertFalse("No vite config should be created",
                new File(baseDir, "vite.config.ts").exists());
        Assert.assertFalse("No types should be created",
                new File(baseDir, "types.d.ts").exists());
        Assert.assertFalse("No tsconfig should be created",
                new File(baseDir, "tsconfig.json").exists());
        Assert.assertFalse("No package-lock.yaml should be created",
                new File(baseDir, "package-lock.yaml").exists());
        Assert.assertFalse("No .npmrc should be created",
                new File(baseDir, ".npmrc").exists());
        Assert.assertFalse("No .pnpmfile.cjs should be created",
                new File(baseDir, ".pnpmfile.cjs").exists());
    }
}
