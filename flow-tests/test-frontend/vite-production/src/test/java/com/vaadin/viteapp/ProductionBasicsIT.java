package com.vaadin.viteapp;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Dimension;

public class ProductionBasicsIT extends ChromeBrowserTest {

    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Test
    public void applicationStarts() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("This place intentionally left empty",
                header.getText());
        Assert.assertFalse((Boolean) getCommandExecutor()
                .executeScript("return Vaadin.developmentMode"));
    }

    @Test
    public void applicationHasThemeAndAssets() {
        getDriver().get(getRootURL());
        waitForDevServer();

        String pColor = $("p").first().getCssValue("color");
        Assert.assertEquals("rgba(0, 100, 0, 1)", pColor);

        Dimension size = $("img").first().getSize();
        Assert.assertEquals(200, size.getWidth());
        Assert.assertEquals(200, size.getHeight());
    }
}
