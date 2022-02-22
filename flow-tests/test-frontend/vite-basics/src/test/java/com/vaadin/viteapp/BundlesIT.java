package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

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
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
