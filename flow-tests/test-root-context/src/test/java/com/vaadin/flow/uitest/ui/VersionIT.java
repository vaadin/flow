package com.vaadin.flow.uitest.ui;

import java.util.regex.Matcher;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.server.Version;
import com.vaadin.testbench.By;

public class VersionIT extends ChromeBrowserTest {
    @Test
    public void versionInfoAvailableInDevelopmentMopde() {
        open();
        WebElement version = findElement(By.id("version"));
        Assert.assertEquals("version: "+Version.getFullVersion(), version.getText());
    }

    @Test
    public void versionInfoNotAvailableInProductionMode() {
        openProduction();
        WebElement version = findElement(By.id("version"));
        Assert.assertEquals("versionInfoMethod not published", version.getText());
    }
}
