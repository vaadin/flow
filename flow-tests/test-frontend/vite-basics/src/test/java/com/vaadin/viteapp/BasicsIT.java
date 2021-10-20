package com.vaadin.viteapp;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.github.bonigarcia.wdm.WebDriverManager;

public class BasicsIT extends ChromeBrowserTest {

    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Test
    public void applicationStarts() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("This place intentionally left empty", header.getText());
    }

    @Test
    @Ignore("Doesn't work from Maven for some reason")
    public void debugWindowShown() {
        getDriver().get(getRootURL());
        waitForDevServer();
        Assert.assertTrue($("vaadin-devmode-gizmo").exists());

        TestBenchElement gizmo = $("vaadin-devmode-gizmo").first();
        gizmo.click();
        Assert.assertNotNull(gizmo.$("div").attributeContains("class", "window").attributeContains("class", "visible")
                .waitForFirst());
    }
}
