package com.vaadin.viteapp;

import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

import org.junit.Assert;
import org.junit.Test;

public class BasicsIT extends ViteDevModeIT {

    @Test
    public void applicationStarts() {
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("This place intentionally left empty",
                header.getText());
    }

    @Test
    public void imageFromThemeShown() {
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
    public void applicationUsesVite() {
        TestBenchElement viteStatus = $(ParagraphElement.class).id("status");
        Assert.assertEquals("Vite feature is true", viteStatus.getText());
    }

    @Test
    public void debugWindowShown() {
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();
        devTools.expand();
        Assert.assertNotNull(
                devTools.$("div").attributeContains("class", "window")
                        .attributeContains("class", "visible").waitForFirst());
    }

    @Test
    public void canImportJson() {
        $("button").id(MainView.LOAD_AND_SHOW_JSON).click();
        Assert.assertEquals("{\"hello\":\"World\"}",
                $("*").id(MainView.JSON_CONTAINER).getText());
    }

    @Test
    public void componentCssDoesNotLeakToDocument() {
        String bodyColor = $("body").first().getCssValue("backgroundColor");
        Assert.assertTrue(
                "Body should be grey, not red as specified for the component",
                bodyColor.contains("211, 211, 211"));
    }
}
