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

package com.vaadin.flow.misc.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WindowType;

import com.vaadin.flow.component.html.testbench.DivElement;
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
        int preserveUIid = getPreserveUIId();

        driver.switchTo().window(activeUIsHandle);
        List<Integer> activeUIs = getActivePreserveUIs();
        Assert.assertTrue("PreserveOnRefresh UI is not listed as active",
                activeUIs.contains(preserveUIid));

        driver.switchTo().window(preserverHandle);
        $(NativeButtonElement.class).id("reload").click();
        int preserveUIidAfterReload = getPreserveUIId();

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

    private String openPreserveOnRefreshView(WindowType windowType) {
        String url = getTestURL().replace("/active-uis", "/preserve");
        driver.switchTo().newWindow(WindowType.TAB).get(url);
        return driver.getWindowHandle();
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
}
