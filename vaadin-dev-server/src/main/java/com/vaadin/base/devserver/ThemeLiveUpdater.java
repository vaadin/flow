/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.CssBundler;
import com.vaadin.flow.server.frontend.ThemeUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Watches the given theme folder for changes, combines the theme on changes and
 * pushes the new version to the browser.
 *
 * @since 24.1
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
                                            CssBundler.inlineImports(
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
