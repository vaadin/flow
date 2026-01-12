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
package com.vaadin.base.devserver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.CssBundler;
import com.vaadin.flow.internal.ThemeUtils;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Watches the given theme folder for changes, combines the theme on changes and
 * pushes the new version to the browser.
 *
 */
public class ThemeLiveUpdater implements Closeable {

    private FileWatcher watcher;

    /**
     * Starts watching the given theme folder (containing styles.css).
     *
     * @param themeFolder
     *            the folder to watch
     * @param context
     *            the current context
     */
    public ThemeLiveUpdater(File themeFolder, VaadinContext context) {
        String themeName = themeFolder.getName();
        File stylesCss = new File(themeFolder, "styles.css");
        JsonNode themeJson = ThemeUtils
                .getThemeJson(themeName, ApplicationConfiguration.get(context))
                .orElse(null);

        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);
        if (liveReload.isPresent()) {
            try {
                watcher = new FileWatcher(file -> {
                    if (file.getName().endsWith(".css")) {
                        try {

                            // All changes are merged into one style block
                            liveReload.get()
                                    .update(ThemeUtils.getThemeFilePath(
                                            themeName, "styles.css"),
                                            CssBundler.inlineImportsForThemes(
                                                    stylesCss.getParentFile(),
                                                    stylesCss, themeJson));
                        } catch (IOException e) {
                            getLogger().error(
                                    "Unable to perform hot update of " + file,
                                    e);
                            liveReload.get().reload();
                        }
                    } else {
                        liveReload.get().reload();
                    }
                }, themeFolder);
                watcher.start();
                getLogger().debug("Watching {} for theme changes", themeFolder);
            } catch (IOException e) {
                getLogger().error("Unable to watch {} for theme changes",
                        themeFolder, e);
            }
        } else {
            getLogger().error(
                    "Browser live reload is not available. Unable to watch {} for theme changes",
                    themeFolder);
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    /**
     * Stops watching the folder and cleans up resources.
     */
    @Override
    public void close() throws IOException {
        watcher.stop();
        watcher = null;
    }

}
