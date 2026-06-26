/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package org.vaadin.example;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.Version;
import com.vaadin.flow.spring.test.SpringDevToolsReloadUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.annotations.BrowserConfiguration;
import com.vaadin.testbench.parallel.Browser;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Class for testing reload time of tiny Vaadin app triggered by spring-boot Dev
 * Tool.
 */
@NotThreadSafe
public class SpringDevToolsReloadViewIT extends ChromeBrowserTest {

    @Test
    public void testSpringBootReloadTime_withNativeButton() {
        String result = SpringDevToolsReloadUtils
                .runAndCalculateAverageResult(5, () -> {
                    open("/reload-nativebutton-test");

                    waitForElementPresent(By.id("start-button"));
                    triggerReload();
                    waitForElementVisible(By.id("result"));

                    return assertAndGetReloadTimeResult();
                });

        System.out.printf(
                "##teamcity[buildStatisticValue key='%s,nativebutton,spring-boot-devtools-reload-time' value='%s']%n",
                getVaadinMajorMinorVersion(), result);

        optionalAssertByReloadThreshold(result);
    }

    @Test
    public void testSpringBootReloadTime_withHorizontalLayout() {
        String result = SpringDevToolsReloadUtils
                .runAndCalculateAverageResult(5, () -> {
                    open("/reload-layout-test");

                    waitForElementPresent(By.id("start-button"));
                    triggerReload();
                    waitForElementVisible(By.id("result"));

                    return assertAndGetReloadTimeResult();
                });

        System.out.printf(
                "##teamcity[buildStatisticValue key='%s,orderedlayout,spring-boot-devtools-reload-time' value='%s']%n",
                getVaadinMajorMinorVersion(), result);

        optionalAssertByReloadThreshold(result);
    }

    @BrowserConfiguration
    public List<DesiredCapabilities> tuneChromeSettings() {
        List<DesiredCapabilities> list = new ArrayList<>();
        DesiredCapabilities desiredCapabilities = Browser.CHROME
                .getDesiredCapabilities();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        list.add(desiredCapabilities.merge(options));
        return list;
    }

    @After
    public void dumpLogs() {
        Logs logs = driver.manage().logs();
        logs.getAvailableLogTypes().stream()
                .flatMap(level -> logs.get(level).getAll().stream())
                .forEach(System.out::println);
    }

    private void triggerReload() {
        findElement(By.id("start-button")).click(); // trigger for reload
    }

    private void open(String testPath) {
        getDriver().get(getTestURL(getRootURL(), testPath, null));
        waitForDevServer();
    }

    private String assertAndGetReloadTimeResult() {
        String reloadTimeInMsText = findElement(By.id("result")).getText();
        Assert.assertNotNull(reloadTimeInMsText);

        return reloadTimeInMsText.substring(reloadTimeInMsText.indexOf("[") + 1,
                reloadTimeInMsText.indexOf("]"));
    }

    private void optionalAssertByReloadThreshold(String reloadTime) {
        if (getReloadTimeAssertThreshold() != null) {
            Assert.assertTrue(String.format(
                    "Reload time %sms was above the threshold %sms. It should stay within the threshold set to system property 'vaadin.test.reload-time-assert-threshold'.",
                    reloadTime, getReloadTimeAssertThreshold()),
                    Double.parseDouble(
                            reloadTime) <= getReloadTimeAssertThreshold());
        }
    }

    private String getVaadinMajorMinorVersion() {
        return Version.getMajorVersion() + "." + Version.getMinorVersion();
    }

    private Double getReloadTimeAssertThreshold() {
        String value = System
                .getProperty("vaadin.test.reload-time-assert-threshold", null);
        if (value != null) {
            return Double.parseDouble(value);
        }
        return null;
    }
}
