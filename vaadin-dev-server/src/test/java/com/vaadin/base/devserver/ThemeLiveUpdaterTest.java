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
package com.vaadin.base.devserver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * The theme push is shared between this watcher and the {@code vaadin-dev}
 * daemon, which suspends the watcher and makes the same call when its
 * {@code apply} decides a change goes live. Both halves are pinned here.
 */
class ThemeLiveUpdaterTest {

    @TempDir
    File temporaryFolder;

    private MockedStatic<ApplicationConfiguration> appConfigStatic;
    private MockedStatic<BrowserLiveReloadAccessor> liveReloadAccessorStatic;

    @AfterEach
    void cleanupStatics() {
        if (appConfigStatic != null) {
            appConfigStatic.close();
        }
        if (liveReloadAccessorStatic != null) {
            liveReloadAccessorStatic.close();
        }
    }

    @Test
    void push_sendsTheCombinedStylesheetUnderTheThemesOwnUrl()
            throws Exception {
        File themeFolder = theme("my-theme");
        VaadinContext context = context(themeFolder);
        BrowserLiveReload liveReload = liveReload(context);

        assertTrue(ThemeLiveUpdater.push(themeFolder, context));

        // The whole point of combining: the browser gets one stylesheet with
        // the imports already inlined, under the URL it knows the theme by.
        Mockito.verify(liveReload).update(
                eq("VAADIN/themes/my-theme/styles.css"),
                argThat(content -> content != null
                        && content.contains(".imported{color:green;}")
                        && content.contains(".main{color:red;}")));
    }

    @Test
    void push_withoutABrowser_saysSoRatherThanThrowing() {
        File themeFolder = new File(temporaryFolder, "gone");
        VaadinContext context = new MockVaadinContext();
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.empty());

        assertFalse(ThemeLiveUpdater.push(themeFolder, context));
    }

    @Test
    void suspend_stopsTheWatcherFromPushingOnSave() throws Exception {
        File themeFolder = theme("my-theme");
        VaadinContext context = context(themeFolder);
        BrowserLiveReload liveReload = liveReload(context);

        // A tool that owns the apply loop decides when a change goes live; a
        // second pusher makes "is my last change live?" unanswerable.
        ThemeLiveUpdater.suspend(context);

        try (ThemeLiveUpdater updater = new ThemeLiveUpdater(themeFolder,
                context)) {
            Files.writeString(new File(themeFolder, "styles.css").toPath(),
                    ".main{color:blue;}", StandardCharsets.UTF_8);
            // The watcher is asynchronous, so absence needs a window in which
            // it would have fired.
            Thread.sleep(500);
        }

        Mockito.verify(liveReload, Mockito.never()).update(anyString(),
                anyString());
        Mockito.verify(liveReload, Mockito.never()).reload();
    }

    @Test
    void close_withoutAWatcher_doesNotThrow() throws Exception {
        // The constructor logs and carries on when the folder cannot be
        // watched, which leaves nothing to stop.
        ThemeLiveUpdater updater = new ThemeLiveUpdater(
                new File(temporaryFolder, "never-created"),
                new MockVaadinContext());

        updater.close();
    }

    private File theme(String name) throws Exception {
        File themeFolder = new File(temporaryFolder,
                "src/main/frontend/themes/" + name);
        assertTrue(themeFolder.mkdirs());
        Files.writeString(new File(themeFolder, "imported.css").toPath(),
                ".imported{color:green;}", StandardCharsets.UTF_8);
        Files.writeString(new File(themeFolder, "styles.css").toPath(),
                "@import './imported.css';\n.main{color:red;}\n",
                StandardCharsets.UTF_8);
        return themeFolder;
    }

    private VaadinContext context(File themeFolder) {
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(temporaryFolder);
        // ThemeUtils resolves the theme folder from the configuration rather
        // than from the folder it is handed, so the two have to agree - as they
        // do in a real application, where both come from the frontend folder.
        Mockito.when(config.getFrontendFolder())
                .thenReturn(new File(temporaryFolder, "src/main/frontend"));
        VaadinContext context = new MockVaadinContext();
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);
        return context;
    }

    private BrowserLiveReload liveReload(VaadinContext context) {
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));
        return liveReload;
    }
}
