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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Class for testing reload time of tiny Spring app triggered by spring-boot Dev
 * Tool.
 */
public class SpringDevToolsReloadViewIT extends ChromeBrowserTest {

    @Test
    public void testPlainSpringBootReloadTime_withHelloWorld() {
        String result = SpringDevToolsReloadUtils
                .runAndCalculateAverageResult(5, () -> {
                    open("/");

                    waitUntil(ExpectedConditions.presenceOfElementLocated(
                            By.id("start-button")), 10);
                    triggerReload();
                    waitUntil(ExpectedConditions
                            .visibilityOfElementLocated(By.id("result")), 10);

                    return assertAndGetReloadTimeResult();
                });

        System.out.printf(
                "##teamcity[buildStatisticValue key='%s,plain-spring-hello-world,spring-boot-devtools-reload-time' value='%s']%n",
                getVaadinMajorMinorVersion(), result);
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
    }

    private String assertAndGetReloadTimeResult() {
        String reloadTimeInMsText = findElement(By.id("result")).getText();
        Assert.assertNotNull(reloadTimeInMsText);

        return reloadTimeInMsText.substring(reloadTimeInMsText.indexOf("[") + 1,
                reloadTimeInMsText.indexOf("]"));
    }

    private String getVaadinMajorMinorVersion() {
        return Version.getMajorVersion() + "." + Version.getMinorVersion();
    }
}
