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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
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
        try {
            watcher = new FileWatcher(file -> {
                if (isSuspended(context)) {
                    return;
                }
                if (file.getName().endsWith(".css")) {
                    push(themeFolder, context);
                } else {
                    BrowserLiveReloadAccessor.getLiveReloadFromContext(context)
                            .ifPresent(BrowserLiveReload::reload);
                }
            }, themeFolder);
            watcher.start();
            getLogger().debug("Watching {} for theme changes", themeFolder);
        } catch (IOException e) {
            getLogger().error("Unable to watch {} for theme changes",
                    themeFolder, e);
        }
    }

    /**
     * Combines the theme and pushes the new stylesheet to the browser, which is
     * what the watcher does when a theme CSS file is saved.
     * <p>
     * Separate from the watcher so a tool that owns the apply loop can perform
     * exactly the same update at the moment it decides a change goes live,
     * rather than reimplementing the combining and the URL. See
     * {@link #suspend(VaadinContext)}.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param themeFolder
     *            the theme folder, whose name is the theme name
     * @param context
     *            the current context
     * @return {@code true} if the new stylesheet reached the browser
     */
    public static boolean push(File themeFolder, VaadinContext context) {
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);
        if (liveReload.isEmpty()) {
            getLogger().error(
                    "Browser live reload is not available. Unable to update {}",
                    themeFolder);
            return false;
        }
        String themeName = themeFolder.getName();
        File stylesCss = new File(themeFolder, "styles.css");
        JsonNode themeJson = ThemeUtils
                .getThemeJson(themeName, ApplicationConfiguration.get(context))
                .orElse(null);
        try {
            // All changes are merged into one style block
            liveReload.get().update(
                    ThemeUtils.getThemeFilePath(themeName, "styles.css"),
                    CssBundler.inlineImportsForThemes(stylesCss.getParentFile(),
                            stylesCss, themeJson));
            return true;
        } catch (IOException e) {
            getLogger().error("Unable to perform hot update of " + stylesCss,
                    e);
            liveReload.get().reload();
            return false;
        }
    }

    /**
     * Stops this watcher from pushing on save, without stopping it.
     * <p>
     * A tool that owns the edit-to-running-app loop decides when a change goes
     * live, and a second watcher pushing on its own makes "what is the state of
     * my last change?" unanswerable. Such a tool suspends this and performs the
     * theme update itself, through {@link #push(File, VaadinContext)}.
     * <p>
     * A context attribute rather than a field, so it can be set before or after
     * the watcher is created - the check happens when a file changes.
     *
     * @param context
     *            the current Vaadin context
     */
    public static void suspend(VaadinContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        context.setAttribute(Suspended.class, new Suspended());
        getLogger().debug(
                "Theme live updates suspended; another tool owns the theme leg");
    }

    private static boolean isSuspended(VaadinContext context) {
        return context.getAttribute(Suspended.class) != null;
    }

    /** Marker for {@link #suspend(VaadinContext)}. */
    private static final class Suspended implements java.io.Serializable {
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ThemeLiveUpdater.class);
    }

    /**
     * Stops watching the folder and cleans up resources.
     */
    @Override
    public void close() throws IOException {
        // Null when the watcher could not be created at all, which is a state
        // the constructor logs and carries on from.
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
    }

}
