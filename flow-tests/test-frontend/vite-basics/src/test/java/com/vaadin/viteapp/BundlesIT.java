/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchElement;
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
        Assert.assertTrue(isExcluded("@testscope/all"));
        Assert.assertTrue(isExcluded("@testscope/button"));
        Assert.assertTrue(isExcluded("@testscope/map"));
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
