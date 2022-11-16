package com.vaadin.viteapp;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.testbench.BrowserTest;

public class BundlesIT extends ViteDevModeIT {

    @BrowserTest
    public void bundlesIsUsed() {
        Assertions.assertTrue((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

    @BrowserTest
    public void bundleExportWorks() {
        Assertions.assertTrue(
                (Boolean) executeScript("return !!window.BundleButtonClass"));
    }

    @BrowserTest // for https://github.com/vaadin/flow/issues/14355
    public void bundleDefaultExportWorks() {
        waitUntilNot(driver -> driver.findElement(By.tagName("testscope-map"))
                .getText().isEmpty());
        Assertions.assertTrue(
                (Boolean) executeScript("return !!window.BundleMapClass"));
        Assertions.assertTrue((Boolean) $("testscope-map").first()
                .getProperty("isFromBundle"));
    }

    @BrowserTest
    public void optimizeDepsConfigHasEntrypoint() {
        Assertions.assertTrue((Boolean) executeScript(
                "return window.ViteConfigOptimizeDeps.entries.includes('generated/vaadin.ts')"));
    }

    @BrowserTest
    public void optimizeDepsExcludesBundles() {
        Assertions.assertTrue(isExcluded("@vaadin/bundles"));
    }

    @BrowserTest
    public void optimizeDepsExcludeBundleContents() {
        Assertions.assertTrue(isExcluded("@testscope/all"));
        Assertions.assertTrue(isExcluded("@testscope/button"));
        Assertions.assertTrue(isExcluded("@testscope/map"));
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
