package com.vaadin.viteapp;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Dimension;

public class ProductionBasicsIT extends ChromeBrowserTest {

    @Test
    public void applicationStarts() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("This place intentionally left empty",
                header.getText());
        Assert.assertFalse((Boolean) getCommandExecutor()
                .executeScript("return Vaadin.developmentMode || false"));
    }

    @Test
    public void imageFromThemeShown() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement img = $("img").id(MainView.PLANT);
        waitUntil(driver -> {
            String heightString = (String) executeScript(
                    "return getComputedStyle(arguments[0]).height.replace('px','')",
                    img);
            float height = Float.parseFloat(heightString);
            return (height > 150);
        });
    }

    @Test
    public void imageCanBeHidden() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement img = $("img").id(MainView.PLANT);
        TestBenchElement button = $("button").id(MainView.HIDEPLANT);
        button.click();
        Assert.assertEquals("none", img.getCssValue("display"));
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

    @Test
    public void toplevelAwaitWorks() {
        getDriver().get(getRootURL());
        waitForDevServer();
        String value = waitUntil(driver -> (String) executeScript(
                "return window.topLevelAwaitValue"));

        Assert.assertEquals("This is the value set in other.js", value);
    }

}
