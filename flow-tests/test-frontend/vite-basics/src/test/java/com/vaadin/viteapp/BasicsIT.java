package com.vaadin.viteapp;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

public class BasicsIT extends ViteDevModeIT {

    @BrowserTest
    public void applicationStarts() {
        TestBenchElement header = $("h2").first();
        Assertions.assertEquals("This place intentionally left empty",
                header.getText());
    }

    @BrowserTest
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

    @BrowserTest
    public void applicationUsesVite() {
        TestBenchElement viteStatus = $(ParagraphElement.class).id("status");
        Assertions.assertEquals("Vite feature is true", viteStatus.getText());
    }

    @BrowserTest
    public void debugWindowShown() {
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();
        devTools.expand();
        Assertions.assertNotNull(
                devTools.$("div").attributeContains("class", "window")
                        .attributeContains("class", "visible").waitForFirst());
    }

    @BrowserTest
    public void canImportJson() {
        $("button").id(MainView.LOAD_AND_SHOW_JSON).click();
        Assertions.assertEquals("{\"hello\":\"World\"}",
                $("*").id(MainView.JSON_CONTAINER).getText());
    }

    @BrowserTest
    public void componentCssDoesNotLeakToDocument() {
        String bodyColor = $("body").first().getCssValue("backgroundColor");
        Assertions.assertTrue(bodyColor.contains("211, 211, 211"),
                "Body should be grey, not red as specified for the component");
    }

    @BrowserTest
    public void importFromDirectoryWorks() {
        String importResult = $("div").id("directoryImportResult").getText();
        Assertions.assertEquals("Directory import ok", importResult);
    }
}
