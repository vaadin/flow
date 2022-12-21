package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;

public class BundlesIT extends ChromeBrowserTest {

    @BeforeAll
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @BrowserTest
    public void bundlesIsNotUsed() {
        getDriver().get(getRootURL());
        waitForClientRouter();
        Assertions.assertFalse((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

}
