/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.example;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.server.Version;
import com.vaadin.flow.spring.test.SpringDevToolsReloadUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.annotations.BrowserConfiguration;
import com.vaadin.testbench.parallel.Browser;

/**
 * Class for testing reload time of "larger" (size is configurable and test is
 * meant for large apps) Vaadin app triggered by spring-boot Dev Tool.
 */
public class SpringDevToolsReloadViewIT extends ChromeBrowserTest {

    @Test
    public void testSpringBootReloadTime_withLargerApp() {
        optionalAssertByReloadThreshold(printTestResultToLog(
                SpringDevToolsReloadUtils.runAndCalculateAverageResult(5,
                        this::runTestReturnResult)));
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

    private String runTestReturnResult() {
        if (hasRouteHierarchy()) {
            open("/catalog/prod/0");
        } else {
            open("/app");
        }

        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.id("start-button")), 20);
        triggerReload();
        waitUntil(
                ExpectedConditions.visibilityOfElementLocated(By.id("result")),
                20);

        return assertAndGetReloadTimeResult();
    }

    private String printTestResultToLog(String result) {
        System.out.printf(
                "##teamcity[buildStatisticValue key='%s,app%s%s,%s-routes,%s-services-per-route%s%s,spring-boot-devtools-reload-time' value='%s']%n",
                getVaadinMajorMinorVersion(),
                (hasRouteHierarchy() ? ",route-hierarchy-enabled" : ""),
                (hasIncludeAddons() ? ",include-addons" : ""),
                getNumberOfGeneratedRoutesProperty(),
                getNumberOfGeneratedServicesPerRouteProperty(),
                (hasCssImports()
                        ? "," + getNumberOfGeneratedCssImportsPerRouteProperty()
                                + "-css-imports-per-route"
                        : ""),
                (hasJsModules()
                        ? "," + getNumberOfGeneratedJsModulesPerRouteProperty()
                                + "-js-modules-per-route"
                        : ""),
                result);

        return result;
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

    private String getNumberOfGeneratedRoutesProperty() {
        return System.getProperty("vaadin.test.codegen.maven.plugin.routes",
                "500");
    }

    private String getNumberOfGeneratedServicesPerRouteProperty() {
        return System.getProperty(
                "vaadin.test.codegen.maven.plugin.services.per.route", "1");
    }

    private String getNumberOfGeneratedCssImportsPerRouteProperty() {
        return System.getProperty(
                "vaadin.test.codegen.maven.plugin.cssimports.per.route", "0");
    }

    private String getNumberOfGeneratedJsModulesPerRouteProperty() {
        return System.getProperty(
                "vaadin.test.codegen.maven.plugin.jsmodules.per.route", "0");
    }

    private boolean hasCssImports() {
        String value = getNumberOfGeneratedCssImportsPerRouteProperty();
        return Integer.parseInt(value) > 0;
    }

    private boolean hasJsModules() {
        String value = getNumberOfGeneratedJsModulesPerRouteProperty();
        return Integer.parseInt(value) > 0;
    }

    private boolean hasRouteHierarchy() {
        return "true".equalsIgnoreCase(
                System.getProperty("route.hierarchy.enabled", "false"));
    }

    private boolean hasIncludeAddons() {
        return "true".equalsIgnoreCase(System.getProperty(
                "vaadin.test.codegen.maven.plugin.include.addons", "false"));
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
