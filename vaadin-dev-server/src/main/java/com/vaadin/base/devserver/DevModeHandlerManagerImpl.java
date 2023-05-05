/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import jakarta.servlet.annotation.HandlesTypes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.startup.DevModeInitializer;
import com.vaadin.base.devserver.startup.DevModeStartupListener;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.CssBundler;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.ThemeUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.VaadinInitializerException;

/**
 * Provides API to access to the {@link DevModeHandler} instance.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeHandlerManagerImpl implements DevModeHandlerManager {

    /*
     * Attribute key for storing Dev Mode Handler startup flag.
     *
     * If presented in Servlet Context, shows the Dev Mode Handler already
     * started / become starting. This attribute helps to avoid Dev Mode running
     * twice.
     *
     * Addresses the issue https://github.com/vaadin/spring/issues/502
     */
    private static final class DevModeHandlerAlreadyStartedAttribute
            implements Serializable {
    }

    private DevModeHandler devModeHandler;
    private BrowserLauncher browserLauncher;
    private FileWatcher themeFilesWatcher;

    @Override
    public Class<?>[] getHandlesTypes() {
        return DevModeStartupListener.class.getAnnotation(HandlesTypes.class)
                .value();
    }

    @Override
    public void setDevModeHandler(DevModeHandler devModeHandler) {
        if (this.devModeHandler != null) {
            throw new IllegalStateException(
                    "Unable to initialize dev mode handler. A handler is already present: "
                            + this.devModeHandler);
        }
        this.devModeHandler = devModeHandler;
    }

    @Override
    public DevModeHandler getDevModeHandler() {
        return devModeHandler;
    }

    @Override
    public void initDevModeHandler(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException {
        setDevModeHandler(
                DevModeInitializer.initDevModeHandler(classes, context));
        startWatchingThemeFolder(context);
        setDevModeStarted(context);
        this.browserLauncher = new BrowserLauncher(context);
    }

    private void startWatchingThemeFolder(VaadinContext context) {
        ApplicationConfiguration config = ApplicationConfiguration.get(context);

        if (config.getMode() != Mode.DEVELOPMENT_BUNDLE) {
            // Theme files are watched by Vite or app runs in prod mode
            return;
        }

        try {
            Optional<String> maybeThemeName = ThemeUtils.getThemeName(context,
                    config);

            if (maybeThemeName.isEmpty()) {
                getLogger().debug("Found no custom theme in the project. "
                        + "Skipping watching the theme files");
                return;
            }
            String themeName = maybeThemeName.get();
            File themeFolder = ThemeUtils.getThemeFolder(
                    FrontendUtils.getProjectFrontendDir(config), themeName);
            File stylesCss = new File(themeFolder, "styles.css");

            Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                    .getLiveReloadFromContext(context);
            if (liveReload.isPresent()) {
                themeFilesWatcher = new FileWatcher(file -> {
                    if (file.getName().endsWith(".css")) {
                        try {
                            // All changes are merged into one style block
                            liveReload.get()
                                    .update(ThemeUtils.getThemeFilePath(
                                            themeName, "styles.css"),
                                            CssBundler.inlineImports(
                                                    stylesCss.getParentFile(),
                                                    stylesCss));
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
                themeFilesWatcher.start();
            } else {
                getLogger().error(
                        "Browser live reload is not available. Failed to start live-reload for theme files");
            }
        } catch (Exception e) {
            getLogger().error("Failed to start live-reload for theme files", e);
        }
    }

    public void stopDevModeHandler() {
        if (devModeHandler != null) {
            devModeHandler.stop();
            devModeHandler = null;
        }
        if (themeFilesWatcher != null) {
            try {
                themeFilesWatcher.stop();
            } catch (Exception e) {
                getLogger().error("Failed to stop theme files watcher", e);
            }
        }
    }

    @Override
    public void launchBrowserInDevelopmentMode(String url) {
        browserLauncher.launchBrowserInDevelopmentMode(url);
    }

    private void setDevModeStarted(VaadinContext context) {
        context.setAttribute(DevModeHandlerAlreadyStartedAttribute.class,
                new DevModeHandlerAlreadyStartedAttribute());
    }

    /**
     * Shows whether {@link DevModeHandler} has been already started or not.
     *
     * @param context
     *            The {@link VaadinContext}, not <code>null</code>
     * @return <code>true</code> if {@link DevModeHandler} has already been
     *         started, <code>false</code> - otherwise
     */
    public static boolean isDevModeAlreadyStarted(VaadinContext context) {
        assert context != null;
        return context.getAttribute(
                DevModeHandlerAlreadyStartedAttribute.class) != null;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeHandlerManagerImpl.class);
    }
}
