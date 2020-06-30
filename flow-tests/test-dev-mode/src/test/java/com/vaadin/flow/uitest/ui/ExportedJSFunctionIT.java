package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
    @Ignore
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
    @Ignore
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
    @Ignore
    public void pollUsingJSProduction() {
        openProduction();
        poll();
    }


    @Test
    public void profilingInfoAvailableInDevelopmentMode() {
        open();
        $(TestBenchElement.class).id("poll").click();
        List<Long> profilingData = getProfilingData();
        assertProfilingDataSensible(profilingData);
    }

    @Test(expected = IllegalStateException.class)
    @Ignore
    public void profilingInfoNotAvailableInProduction() {
        openProduction();
        getProfilingData();
    }

    @Test
    @Ignore
    public void profilingInfoAvailableWhenRequestedInProduction() {
        openProductionWithTiming();
        $(TestBenchElement.class).id("poll").click();
        assertProfilingDataSensible(getProfilingData());
    }

    private void assertProfilingDataSensible(List<Long> profilingData) {
        Assert.assertEquals(5, profilingData.size());
        // Time rendering the poll response can be 0ms
        Assert.assertTrue(profilingData.get(0) >= 0);
        for (int i = 1; i < 5; i++)
            Assert.assertTrue(profilingData.get(i) > 0);
    }

    private List<Long> getProfilingData() {
        Object data = executeScript(
                "var key = Object.keys(Vaadin.Flow.clients).filter(k => k !== 'TypeScript')[0];" +
                "return Vaadin.Flow.clients[key].getProfilingData();");
        if (data == null) {
            throw new IllegalStateException("No profiling data available");
        }
        return (List<Long>) data;
    }

}
