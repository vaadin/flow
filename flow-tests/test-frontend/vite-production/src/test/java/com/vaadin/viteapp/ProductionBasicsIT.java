package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.Dimension;

import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

public class ProductionBasicsIT extends ChromeBrowserTest {

    @BeforeAll
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @BrowserTest
    public void applicationStarts() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement header = $("h2").first();
        Assertions.assertEquals("This place intentionally left empty",
                header.getText());
        Assertions.assertFalse((Boolean) getCommandExecutor()
                .executeScript("return Vaadin.developmentMode"));
    }

    @BrowserTest
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

    @BrowserTest
    public void imageCanBeHidden() {
        getDriver().get(getRootURL());
        waitForDevServer();
        TestBenchElement img = $("img").id(MainView.PLANT);
        TestBenchElement button = $("button").id(MainView.HIDEPLANT);
        button.click();
        Assertions.assertEquals("none", img.getCssValue("display"));
    }

    @BrowserTest
    public void applicationHasThemeAndAssets() {
        getDriver().get(getRootURL());
        waitForDevServer();

        String pColor = $("p").first().getCssValue("color");
        Assertions.assertEquals("rgba(0, 100, 0, 1)", pColor);

        Dimension size = $("img").first().getSize();
        Assertions.assertEquals(200, size.getWidth());
        Assertions.assertEquals(200, size.getHeight());
    }
}
