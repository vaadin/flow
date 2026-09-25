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

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

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
 * That the dev loop's usage-statistics entries actually reach a page.
 * <p>
 * The entries are snapshotted into the bootstrap document when it is rendered,
 * so this navigates for each assertion rather than watching one open page -
 * which is why it is not a case in {@link DevLoopBrowserIT}, where nothing
 * reloads on purpose.
 */
@Isolated
class DevLoopSpringStatisticsIT extends BrowserTestBase
        implements DriverSupplier {

    private static final Path VIEW = AbstractDevLoopIT.MUTABLE
            .resolve("TaskListView.java");

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
    void startTheApplication() {
        cli = VaadinDevCli.of(AbstractDevLoopIT.APP);
        patch = new SourcePatch();
        cli.run("start").assertExitCode(0);
        cli.run("apply").assertExitCode(0);
    }

    @AfterEach
    void revert() {
        patch.close();
        if (cli != null) {
            cli.run("apply");
        }
    }

    @BrowserTest
    void anApplicationUnderTheLoop_reportsTheLoopAndThenTheApply() {
        // The daemon deliberately survives between tests, so the running JVM
        // may already have applied something. A restart is what makes the
        // "not yet" assertion below mean anything.
        cli.run("restart").assertExitCode(0);

        openTheView();
        Assertions.assertTrue(registered("flow/devloop"),
                "an application the daemon launched should report flow/devloop, "
                        + "but the page registered " + registrationNames());
        Assertions.assertFalse(registered("flow/devloop/apply"),
                "a freshly launched application has hot swapped nothing yet, "
                        + "but the page registered " + registrationNames());

        patch.replace(VIEW, "\"Task List\"", "\"Tasks, hot swapped\"");
        cli.run("apply").assertExitCode(0).assertOutputContains("hot-reload:");

        // The entry was marked during the redefine, and the snapshot is taken
        // when the document is rendered - so it is the next page load that
        // carries it.
        openTheView();
        Assertions.assertTrue(registered("flow/devloop/apply"),
                "a hot-swapped change should report flow/devloop/apply, but "
                        + "the page registered " + registrationNames());
    }

    private void openTheView() {
        getDriver()
                .get("http://localhost:" + AbstractDevLoopIT.SERVER_PORT + "/");
        // The first snapshot after navigating is usually empty - Vaadin renders
        // client-side - so wait for something the view owns.
        new WebDriverWait(getDriver(), Duration.ofMinutes(2))
                .until(driver -> !driver.findElements(By.cssSelector("#title"))
                        .isEmpty());
    }

    private boolean registered(String name) {
        return registrationNames().contains(name);
    }

    @SuppressWarnings("unchecked")
    private List<String> registrationNames() {
        return (List<String>) ((JavascriptExecutor) getDriver()).executeScript(
                "return (window.Vaadin && window.Vaadin.registrations || [])"
                        + ".map(function(entry) { return entry.is; });");
    }
}
