package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BundlesIT extends ChromeBrowserTest {

    @Test
    public void bundlesIsNotUsed() {
        getDriver().get(getRootURL());
        waitForClientRouter();
        Assert.assertFalse((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

}
