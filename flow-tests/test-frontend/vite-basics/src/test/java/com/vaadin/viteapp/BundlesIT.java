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
import org.openqa.selenium.By;

public class BundlesIT extends ViteDevModeIT {

    @Test
    public void bundlesIsUsed() {
        Assert.assertTrue((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

    @Test
    public void bundleExportWorks() {
        Assert.assertTrue(
                (Boolean) executeScript("return !!window.BundleButtonClass"));
    }

    @Test // for https://github.com/vaadin/flow/issues/14355
    public void bundleDefaultExportWorks() {
        waitUntilNot(driver -> driver.findElement(By.tagName("testscope-map"))
                .getText().isEmpty());
        Assert.assertTrue(
                (Boolean) executeScript("return !!window.BundleMapClass"));
        Assert.assertTrue((Boolean) $("testscope-map").first()
                .getProperty("isFromBundle"));
    }

    @Test
    public void optimizeDepsConfigHasEntrypoint() {
        Assert.assertTrue((Boolean) executeScript(
                "return window.ViteConfigOptimizeDeps.entries.includes('generated/vaadin.ts')"));
    }

    @Test
    public void optimizeDepsExcludesBundles() {
        Assert.assertTrue(isExcluded("@vaadin/bundles"));
    }

    @Test
    public void optimizeDepsExcludeBundleContents() {
        Assert.assertTrue(isExcluded("@vaadin/testscope-all"));
        Assert.assertTrue(isExcluded("@vaadin/testscope-button"));
        Assert.assertTrue(isExcluded("@vaadin/testscope-map"));
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
