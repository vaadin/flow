/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.remote.Command;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchDriverProxy;

public class ReactNavigateIT extends ChromeBrowserTest {

    @Test
    public void testSlowNavigation_clickReactNavigateButtonTwice_noExceptionInLogs()
            throws IOException {
        open();
        ChromeDriver driver = (ChromeDriver) ((TestBenchDriverProxy) getDriver())
                .getWrappedDriver();

        CommandExecutor executor = driver.getCommandExecutor();
        Map map = new HashMap();
        map.put("offline", false);
        map.put("latency", 100);

        map.put("download_throughput", 1800);
        map.put("upload_throughput", 400);

        executor.execute(new Command(driver.getSessionId(),
                "setNetworkConditions", ImmutableMap.of("network_conditions",
                        ImmutableMap.copyOf(map))));

        waitUntil(drvr -> $(NativeButtonElement.class).id("react-navigate")
                .isDisplayed());

        NativeButtonElement navigateButton = $(NativeButtonElement.class)
                .id("react-navigate");

        // timout and requestAnimationFrame will give an undefined for the
        // button.
        driver.executeScript("arguments[0].click();arguments[0].click();",
                navigateButton);

        waitUntil(ExpectedConditions.stalenessOf(navigateButton));

        checkLogsForErrors();

    }

}
