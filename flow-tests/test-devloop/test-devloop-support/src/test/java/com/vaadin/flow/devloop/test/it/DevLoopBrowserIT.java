/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.devloop.test.it;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Isolated;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.DriverSupplier;

/**
 * The half only a browser can answer: the exit code proves the bytes are live,
 * and this proves the already-open page shows them.
 * <p>
 * The page is navigated to once and then left alone: a CSS push and a Java hot
 * swap both land in an open page, and re-navigating would hide exactly what is
 * being tested. Nothing here reloads.
 * <p>
 * Run by both fixtures, and worth the browser in both: what reaches the open
 * page travels over the dev-tools connection the application itself serves, so
 * a servlet container that never finished the websocket handshake, or a webapp
 * class loader whose redefined component Flow cannot find to refresh, is a
 * failure no exit code would show. Neither fixture runs a dev server - both set
 * {@code vaadin.frontend.hotdeploy=false} so that nothing but the daemon can
 * push - which is what makes the same assertions meaningful on both.
 * <p>
 * This class does not extend {@link AbstractDevLoopIT}: TestBench's
 * {@link BrowserTestBase} is the superclass a {@link BrowserTest} needs, so the
 * fixture paths and the CLI are used through it instead of inherited.
 */
@Isolated
class DevLoopBrowserIT extends BrowserTestBase implements DriverSupplier {

    private VaadinDevCli cli;
    private SourcePatch patch;

    @Override
    public WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu",
                "--disable-backgrounding-occluded-windows");
        // Required in CI and containers, which disable the Chrome sandbox and
        // have a small /dev/shm.
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }

    @BeforeEach
    void openTheApplication() {
        cli = VaadinDevCli.of(AbstractDevLoopIT.APP);
        patch = new SourcePatch();
        cli.run("start").assertExitCode(0);
        cli.run("apply").assertExitCode(0);
        getDriver()
                .get("http://localhost:" + AbstractDevLoopIT.SERVER_PORT + "/");
        // The first snapshot after navigating is usually empty - Vaadin renders
        // client-side - so wait for something the view owns.
        waitFor("#title");
    }

    @AfterEach
    void revert() {
        patch.close();
        if (cli != null) {
            cli.run("apply");
        }
    }

    @BrowserTest
    void viewEdit_changesTheOpenPageWithoutAReload() {
        Assertions.assertEquals("Task List", text("#title"));
        String reloadMarker = markPage();

        patch.replace(AbstractDevLoopIT.VIEW, "\"Task List\"",
                "\"Tasks, hot swapped\"");
        cli.run("apply").assertExitCode(0).assertOutputContains("hot-reload:");

        // onHotswap re-creates the component, so the new text arrives on its
        // own.
        new WebDriverWait(getDriver(), Duration.ofSeconds(30))
                .until(driver -> "Tasks, hot swapped".equals(text("#title")));
        Assertions.assertEquals(reloadMarker, currentMarker(),
                "the page must not have reloaded");
    }

    @BrowserTest
    void cssEdit_changesTheComputedStyleWithoutAReload() {
        String reloadMarker = markPage();

        patch.replace(AbstractDevLoopIT.STYLESHEET, "row-gap: 12px;",
                "row-gap: 37px;");
        cli.run("apply").assertExitCode(0).assertOutputContains("hmr:");

        // Computed style, not a screenshot: this is the assertion that says the
        // stylesheet the browser is using is the edited one.
        new WebDriverWait(getDriver(), Duration.ofSeconds(30))
                .until(driver -> "37px"
                        .equals(computedStyle(".task-list-view", "rowGap")));
        Assertions.assertEquals(reloadMarker, currentMarker(),
                "a CSS push must not reload the page");
    }

    @BrowserTest
    void cssRuleRemoved_stopsApplyingToTheOpenPage() {
        // The value change above passes whether or not the push replaced the
        // stylesheet the bootstrap linked: a <style> added to the end of <head>
        // outranks an earlier <link> on document order, so a new value wins
        // either way. Removing a declaration is the case that needs the old
        // <link> actually gone, which is why it is asserted separately.
        Assertions.assertEquals("12px",
                computedStyle(".task-list-view", "rowGap"),
                "the fixture should start from the stylesheet's own value");
        String reloadMarker = markPage();

        patch.replace(AbstractDevLoopIT.STYLESHEET, "row-gap: 12px;", "");
        cli.run("apply").assertExitCode(0).assertOutputContains("hmr:");

        // Back to the initial value of row-gap: nothing declares it any more.
        new WebDriverWait(getDriver(), Duration.ofSeconds(30))
                .withMessage(() -> "row-gap should have fallen back to its "
                        + "initial value once the declaration was removed, but "
                        + "is still "
                        + computedStyle(".task-list-view", "rowGap")
                        + " - the stylesheet the page bootstrapped with is "
                        + "still applying")
                .until(driver -> "normal"
                        .equals(computedStyle(".task-list-view", "rowGap")));
        Assertions.assertEquals(reloadMarker, currentMarker(),
                "a CSS push must not reload the page");
    }

    @BrowserTest
    void cssAndJavaInOneApply_reportBothHalvesAndBothLandInTheOpenPage() {
        // The case the reporting used to lose: the resource leg pushes and the
        // Java leg redefines, and with a page open the push's own words are
        // what says the stylesheet arrived. Asserted here rather than in
        // DevLoopCssIT because "no browser connected" is the only answer a
        // headless apply can get.
        Assertions.assertEquals("Task List", text("#title"));
        String reloadMarker = markPage();

        patch.replace(AbstractDevLoopIT.STYLESHEET, "row-gap: 12px;",
                "row-gap: 41px;");
        patch.replace(AbstractDevLoopIT.VIEW, "\"Task List\"",
                "\"Tasks, mixed\"");
        cli.run("apply").assertExitCode(0)
                .assertOutputContains(
                        "hmr: 1 resource(s) copied, pushed 1 stylesheet(s)"
                                + " in place")
                .assertOutputContains("hot-reload:");

        new WebDriverWait(getDriver(), Duration.ofSeconds(30))
                .until(driver -> "41px"
                        .equals(computedStyle(".task-list-view", "rowGap")));
        new WebDriverWait(getDriver(), Duration.ofSeconds(30))
                .until(driver -> "Tasks, mixed".equals(text("#title")));
        Assertions.assertEquals(reloadMarker, currentMarker(),
                "neither half of a mixed change-set may reload the page");
    }

    @BrowserTest
    void siblingModuleEdit_isVisibleOnceTheViewRendersAgain() {
        patch.replace(AbstractDevLoopIT.FORMATTER, "FormatStyle.MEDIUM",
                "FormatStyle.FULL");

        cli.run("apply").assertExitCode(0)
                .assertOutputContains("no Vaadin component was redefined");

        // The bytes are live and the rendered text is stale, exactly as apply
        // said: nothing asked the view to render again. Doing that is what
        // makes the change visible - and is what the skill tells an agent to
        // do.
        String stale = text("#due-date");
        getDriver().findElement(By.cssSelector("#refresh")).click();
        new WebDriverWait(getDriver(), Duration.ofSeconds(30))
                .until(driver -> !stale.equals(text("#due-date")));
    }

    private void waitFor(String selector) {
        new WebDriverWait(getDriver(), Duration.ofMinutes(2))
                .until(driver -> !driver.findElements(By.cssSelector(selector))
                        .isEmpty());
    }

    private String text(String selector) {
        return getDriver().findElement(By.cssSelector(selector)).getText()
                .trim();
    }

    private String computedStyle(String selector, String property) {
        return (String) ((JavascriptExecutor) getDriver()).executeScript(
                "return getComputedStyle(document.querySelector(arguments[0]))[arguments[1]];",
                selector, property);
    }

    /**
     * Stamps the window so a reload can be detected: a reload throws the value
     * away, and "the page did not reload" is half of what a hot swap claims.
     */
    private String markPage() {
        String marker = "devloop-" + System.nanoTime();
        ((JavascriptExecutor) getDriver())
                .executeScript("window.devloopMarker = arguments[0];", marker);
        return marker;
    }

    private String currentMarker() {
        return (String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.devloopMarker;");
    }
}
