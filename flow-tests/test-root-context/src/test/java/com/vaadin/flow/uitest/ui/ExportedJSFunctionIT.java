package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ExportedJSFunctionIT extends ChromeBrowserTest {
    @Test
    public void versionInfoAvailableInDevelopmentMopde() {
        open();
        WebElement version = findElement(By.id("version"));
        Assert.assertEquals("version: " + Version.getFullVersion(),
                version.getText());
    }

    @Test
    public void versionInfoNotAvailableInProductionMode() {
        openProduction();
        WebElement version = findElement(By.id("version"));
        Assert.assertEquals("versionInfoMethod not published",
                version.getText());
    }

    @Test
    public void productionModeFalseInDevelopmentMode() {
        open();
        WebElement productionMode = findElement(By.id("productionMode"));
        Assert.assertEquals("Production mode: false", productionMode.getText());
    }

    @Test
    public void productionModeTrueInProductionMode() {
        openProduction();
        WebElement productionMode = findElement(By.id("productionMode"));
        Assert.assertEquals("Production mode: true", productionMode.getText());
    }

    @Test
    public void pollUsingJS() {
        open();
        poll();
    }

    private void poll() {
        TestBenchElement counter = $(TestBenchElement.class).id("pollCounter");
        TestBenchElement pollTrigger = $(TestBenchElement.class).id("poll");

        Assert.assertEquals("No polls", counter.getText());
        pollTrigger.click();
        Assert.assertEquals("Poll called 1 times", counter.getText());
    }

    @Test
    public void pollUsingJSProduction() {
        openProduction();
        poll();
    }
}
