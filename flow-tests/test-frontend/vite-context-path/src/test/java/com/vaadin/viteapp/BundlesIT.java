package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BundlesIT extends ChromeBrowserTest {

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
        Assert.assertFalse(isExcluded("@vaadin/testscope-all"));
        Assert.assertFalse(isExcluded("@vaadin/testscope-button"));
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
