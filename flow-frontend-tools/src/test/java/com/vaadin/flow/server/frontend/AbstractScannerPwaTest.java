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
package com.vaadin.flow.server.frontend.scanner;

import org.junit.Test;

import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.samples.pwa.AnotherAppShellWithPwa;
import com.vaadin.flow.server.frontend.scanner.samples.pwa.AppShellWithPwa;
import com.vaadin.flow.server.frontend.scanner.samples.pwa.AppShellWithoutPwa;
import com.vaadin.flow.server.frontend.scanner.samples.pwa.NonAppShellWithPwa;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class AbstractScannerPwaTest {
    abstract protected PwaConfiguration getPwaConfiguration(Class<?>... classes)
            throws Exception;

    @Test
    public void should_findPwaOnAppShell() throws Exception {
        PwaConfiguration pwaConfiguration = getPwaConfiguration(
                AppShellWithPwa.class);
        assertEquals("PWA Application", pwaConfiguration.getAppName());
        assertEquals("PWA", pwaConfiguration.getShortName());
        assertEquals("Testing PWA", pwaConfiguration.getDescription());
        assertEquals("minimal-ui", pwaConfiguration.getDisplay());
        assertEquals("#eee", pwaConfiguration.getBackgroundColor());
        assertEquals("#369", pwaConfiguration.getThemeColor());
        assertEquals("pwa.png", pwaConfiguration.getIconPath());
        assertEquals("appmanifest.json", pwaConfiguration.getManifestPath());
        assertEquals("pwa.html", pwaConfiguration.getOfflinePath());
        String[] expectedOfflineResources = { "pwa.js", "pwa.css" };
        assertArrayEquals(expectedOfflineResources,
                pwaConfiguration.getOfflineResources().toArray());
    }

    @Test
    public void should_returnDefaultConfiguration_When_AppShellWithoutPwa()
            throws Exception {
        PwaConfiguration pwaConfiguration = getPwaConfiguration(
                AppShellWithoutPwa.class);
        assertEquals(PwaConfiguration.DEFAULT_NAME,
                pwaConfiguration.getAppName());
        assertEquals("Flow PWA", pwaConfiguration.getShortName());
        assertEquals("", pwaConfiguration.getDescription());
        assertEquals(PwaConfiguration.DEFAULT_DISPLAY,
                pwaConfiguration.getDisplay());
        assertEquals(PwaConfiguration.DEFAULT_BACKGROUND_COLOR,
                pwaConfiguration.getBackgroundColor());
        assertEquals(PwaConfiguration.DEFAULT_THEME_COLOR,
                pwaConfiguration.getThemeColor());
        assertEquals(PwaConfiguration.DEFAULT_ICON,
                pwaConfiguration.getIconPath());
        assertEquals(PwaConfiguration.DEFAULT_PATH,
                pwaConfiguration.getManifestPath());
        assertEquals(PwaConfiguration.DEFAULT_OFFLINE_PATH,
                pwaConfiguration.getOfflinePath());
        String[] defaultOfflineResources = {};
        assertArrayEquals(defaultOfflineResources,
                pwaConfiguration.getOfflineResources().toArray());
    }

    @Test
    public void should_returnDefaultConfiguration_When_NoAppShell()
            throws Exception {
        PwaConfiguration pwaConfiguration = getPwaConfiguration(
                this.getClass());
        assertEquals(PwaConfiguration.DEFAULT_NAME,
                pwaConfiguration.getAppName());
        assertEquals("Flow PWA", pwaConfiguration.getShortName());
        assertEquals("", pwaConfiguration.getDescription());
        assertEquals(PwaConfiguration.DEFAULT_DISPLAY,
                pwaConfiguration.getDisplay());
        assertEquals(PwaConfiguration.DEFAULT_BACKGROUND_COLOR,
                pwaConfiguration.getBackgroundColor());
        assertEquals(PwaConfiguration.DEFAULT_THEME_COLOR,
                pwaConfiguration.getThemeColor());
        assertEquals(PwaConfiguration.DEFAULT_ICON,
                pwaConfiguration.getIconPath());
        assertEquals(PwaConfiguration.DEFAULT_PATH,
                pwaConfiguration.getManifestPath());
        assertEquals(PwaConfiguration.DEFAULT_OFFLINE_PATH,
                pwaConfiguration.getOfflinePath());
        String[] defaultOfflineResources = {};
        assertArrayEquals(defaultOfflineResources,
                pwaConfiguration.getOfflineResources().toArray());
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_When_PwaNotOnAppShell() throws Exception {
        getPwaConfiguration(NonAppShellWithPwa.class);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_When_MultipleAppShellPwa() throws Exception {
        getPwaConfiguration(AppShellWithPwa.class,
                AnotherAppShellWithPwa.class);
    }
}
