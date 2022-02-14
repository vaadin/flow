package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.DevModeGizmoElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

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
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).waitForFirst();
        gizmo.expand();
        Assert.assertNotNull(gizmo.$("div").attributeContains("class", "window")
                .attributeContains("class", "visible").waitForFirst());
    }

    @Test
    public void canImportJson() {
        $("button").id(MainView.LOAD_AND_SHOW_JSON).click();
        Assert.assertEquals("{\"hello\":\"World\"}",
                $("*").id(MainView.JSON_CONTAINER).getText());
    }

    @Test
    public void hasNeatNodeModulesPath() {
        final String processedJs = (String) ((JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        "const done = arguments[arguments.length - 1];"
                                + "fetch('frontend/deploader.js', {"
                                + "headers: {Accept: 'application/javascript'}"
                                + "}).then(response => response.text()).then(done);");
        Assert.assertEquals(
                "import('/VAADIN/node_modules/@vaadin/flow-frontend/package.json?import');",
                processedJs);
    }
}
