/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
