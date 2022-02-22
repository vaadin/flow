package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class BundlesIT extends ChromeBrowserTest {

    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Before
    public void openView() {
        getDriver().get(getRootURL() + "/my-context");
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
    }

    @Test
    public void bundlesIsNotUsedWhenHasVersionMismatch() {
        Assert.assertFalse((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

    @Test
    public void optimizeDepsNotExcludeBundleContents() {
        Assert.assertFalse(isExcluded("@testscope/all"));
        Assert.assertFalse(isExcluded("@testscope/button"));
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
