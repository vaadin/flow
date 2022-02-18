package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BundlesIT extends ChromeBrowserTest {

    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Test
    public void bundlesIsNotUsed() {
        getDriver().get(getRootURL());
        waitForClientRouter();
        Assert.assertFalse((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

}
