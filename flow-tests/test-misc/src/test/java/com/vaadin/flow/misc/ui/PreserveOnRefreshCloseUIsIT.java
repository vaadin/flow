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

package com.vaadin.flow.misc.ui;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WindowType;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.H3Element;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PreserveOnRefreshCloseUIsIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/active-uis";
    }

    @Test
    public void preserveOnRefreshView_pageReload_previousUIImmediatelyClosed() {
        open();
        String activeUIsHandle = driver.getWindowHandle();
        String preserverHandle = openPreserveOnRefreshView(WindowType.TAB);
        int initialUIid = getInitialUIId();
        int preserveUIid = getPreserveUIId();

        driver.switchTo().window(activeUIsHandle);
        List<Integer> activeUIs = getActivePreserveUIs();
        Assert.assertTrue("PreserveOnRefresh UI is not listed as active",
                activeUIs.contains(preserveUIid));

        driver.switchTo().window(preserverHandle);
        $(NativeButtonElement.class).id("reload").click();
        int preserveUIidAfterReload = getPreserveUIId();
        Assert.assertEquals("Initial UI ID should not change on reload",
                initialUIid, getInitialUIId());

        driver.switchTo().window(activeUIsHandle);
        activeUIs = getActivePreserveUIs();

        Assert.assertFalse("Previous PreserveOnRefresh UI has not been closed",
                activeUIs.contains(preserveUIid));
        Assert.assertTrue("New PreserveOnRefresh UI is not listed as active",
                activeUIs.contains(preserveUIidAfterReload));
    }

    @Test
    public void preserveOnRefreshView_tabClosed_uiClosedAfterHeartbeatTimeout() {
        open();
        String activeUIsHandle = driver.getWindowHandle();
        String preserverHandle = openPreserveOnRefreshView(WindowType.TAB);
        int preserveUIid = getPreserveUIId();

        driver.switchTo().window(activeUIsHandle);
        List<Integer> activeUIs = getActivePreserveUIs();
        Assert.assertTrue("PreserveOnRefresh UI is not listed as active",
                activeUIs.contains(preserveUIid));

        driver.switchTo().window(preserverHandle);
        driver.close();

        driver.switchTo().window(activeUIsHandle);
        // Verify that the closed tab UI is closed after three heartbeat
        // intervals have elapsed
        waitUntil(d -> !getActivePreserveUIs().contains(preserveUIid),
                CustomServlet.HEARTBEAT_INTERVAL * 4);
    }

    @Test
    public void preserveOnRefreshView_windowClosed_uiClosedAfterHeartbeatTimeout() {
        open();
        String activeUIsHandle = driver.getWindowHandle();
        String preserverHandle = openPreserveOnRefreshView(WindowType.WINDOW);
        int preserveUIid = getPreserveUIId();

        driver.switchTo().window(activeUIsHandle);
        List<Integer> activeUIs = getActivePreserveUIs();
        Assert.assertTrue("PreserveOnRefresh UI is not listed as active",
                activeUIs.contains(preserveUIid));

        driver.switchTo().window(preserverHandle);
        driver.close();

        driver.switchTo().window(activeUIsHandle);
        // Verify that the closed tab UI is closed after three heartbeat
        // intervals have elapsed
        waitUntil(d -> !getActivePreserveUIs().contains(preserveUIid),
                CustomServlet.HEARTBEAT_INTERVAL * 4);
    }

    @Test
    public void preserveOnRefreshUiClosed_weakReferenceCleanedAfterGC() {
        open();
        String activeUIsHandle = driver.getWindowHandle();
        String preserverHandle = openPreserveOnRefreshView(WindowType.TAB);
        List<Integer> expectedGCIds = new ArrayList<>();
        int initialUIid = getInitialUIId();
        int preserveUIid = getPreserveUIId();

        driver.switchTo().window(activeUIsHandle);
        List<Integer> uiIdList = getGCCollectedUIs();
        Assert.assertFalse(
                "PreserveOnRefresh UI has been already collected by GC",
                uiIdList.contains(preserveUIid));

        driver.switchTo().window(preserverHandle);
        for (int i = 0; i < 5; i++) {
            int currentUIid = getPreserveUIId();
            expectedGCIds.add(currentUIid);
            $(NativeButtonElement.class).id("reload").click();
            waitUntil(d -> getPreserveUIId() != currentUIid);
            Assert.assertEquals("Initial UI ID should not change on reload",
                    initialUIid, getInitialUIId());
        }

        driver.switchTo().window(activeUIsHandle);
        // waitUntil(d -> $(NativeButtonElement.class).id("gc-hint")).click();
        waitForElementPresent(By.id("gc-hint"));
        // Give some time to the GC to clean up some memory
        waitUntil(d -> {
            $(NativeButtonElement.class).id("gc-hint").click();
            List<Integer> gcCollectedUIs = getGCCollectedUIs();
            boolean cleaned = gcCollectedUIs.containsAll(expectedGCIds);
            if (!cleaned) {
                LoggerFactory.getLogger(PreserveOnRefreshCloseUIsIT.class)
                        .debug("Not all expected UI have been GC collect yet. Expecting {} UIs to be collected but was {}.",
                                expectedGCIds, gcCollectedUIs);
            }
            return cleaned;
        }, 3);

        // Close preserve view tab and wait for heartbeat cleanup
        driver.switchTo().window(preserverHandle);
        int lastPreserveUIid = getPreserveUIId();
        driver.close();
        driver.switchTo().window(activeUIsHandle);
        waitUntil(d -> !getActivePreserveUIs().contains(lastPreserveUIid),
                CustomServlet.HEARTBEAT_INTERVAL * 4);

        expectedGCIds.add(lastPreserveUIid);
        $(NativeButtonElement.class).id("gc-hint").click();
        // Give some time to the GC to clean up some memory
        waitUntil(d -> getGCCollectedUIs().containsAll(expectedGCIds), 20);
    }

    private String openPreserveOnRefreshView(WindowType windowType) {
        String url = getTestURL().replace("/active-uis", "/preserve");
        driver.switchTo().newWindow(windowType).get(url);
        return driver.getWindowHandle();
    }

    private int getInitialUIId() {
        return Integer.parseInt(waitUntil(d -> $(H3Element.class).single())
                .getText().replace("Initial UI: ", ""));
    }

    private int getPreserveUIId() {
        waitForElementPresent(By.id("uiId"));
        return Integer.parseInt(
                $(DivElement.class).id("uiId").getText().replace("UI: ", ""));
    }

    private List<Integer> getActivePreserveUIs() {
        $(NativeButtonElement.class).id("list-uis").click();
        return $(DivElement.class).id("uis").$(DivElement.class).all().stream()
                .map(TestBenchElement::getText)
                .filter(text -> text.endsWith("Path: preserve"))
                .map(text -> Integer
                        .parseInt(text.replaceFirst("^UI: (\\d+), .*$", "$1")))
                .toList();
    }

    private List<Integer> getGCCollectedUIs() {
        $(NativeButtonElement.class).id("list-gc-collected-uis").click();
        return $(DivElement.class).id("gcuis").$(DivElement.class).all()
                .stream().map(TestBenchElement::getText)
                .map(text -> Integer.parseInt(
                        text.replaceFirst("^GC Collected UI: (\\d+)$", "$1")))
                .toList();
    }

}
