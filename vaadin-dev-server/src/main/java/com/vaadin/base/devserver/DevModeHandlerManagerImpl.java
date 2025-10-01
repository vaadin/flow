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

import jakarta.servlet.annotation.HandlesTypes;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.startup.DevModeInitializer;
import com.vaadin.base.devserver.startup.DevModeStartupListener;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinContext;
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
    private final Set<Command> shutdownCommands = new HashSet<>();
    private ExecutorService executorService;

    private String applicationUrl;
    private boolean fullyStarted = false;

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
        shutdownExecutorService();
        executorService = Executors.newFixedThreadPool(4,
                new NamedDaemonThreadFactory("vaadin-dev-server"));
        setDevModeHandler(DevModeInitializer.initDevModeHandler(classes,
                context, executorService));
        CompletableFuture.runAsync(() -> {
            DevModeHandler devModeHandler = getDevModeHandler();
            if (devModeHandler instanceof AbstractDevServerRunner) {
                ((AbstractDevServerRunner) devModeHandler).waitForDevServer();
            } else if (devModeHandler instanceof DevBundleBuildingHandler devBundleBuilder) {
                devBundleBuilder.waitForDevBundle();
            }

            ApplicationConfiguration config = ApplicationConfiguration
                    .get(context);
            startWatchingThemeFolder(context, config);
            startWatchingPublicResourcesCss(context, config);
            watchExternalDependencies(context, config);
            setFullyStarted(true);
        }, executorService);
        setDevModeStarted(context);
        this.browserLauncher = new BrowserLauncher(context);
    }

    private void shutdownExecutorService() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private void watchExternalDependencies(VaadinContext context,
            ApplicationConfiguration config) {
        File frontendFolder = FrontendUtils.getProjectFrontendDir(config);
        File jarFrontendResourcesFolder = FrontendUtils
                .getJarResourcesFolder(frontendFolder);
        registerWatcherShutdownCommand(new ExternalDependencyWatcher(context,
                jarFrontendResourcesFolder));

    }

    private void startWatchingThemeFolder(VaadinContext context,
            ApplicationConfiguration config) {

        if (config.getMode() != Mode.DEVELOPMENT_BUNDLE) {
            // Theme files are watched by Vite or app runs in prod mode
            return;
        }

        try {
            Optional<String> maybeThemeName = ThemeUtils.getThemeName(context);

            if (maybeThemeName.isEmpty()) {
                getLogger().debug("Found no custom theme in the project. "
                        + "Skipping watching the theme files");
                return;
            }
            List<String> activeThemes = ThemeUtils.getActiveThemes(context);
            for (String themeName : activeThemes) {
                File themeFolder = ThemeUtils.getThemeFolder(
                        FrontendUtils.getProjectFrontendDir(config), themeName);
                registerWatcherShutdownCommand(
                        new ThemeLiveUpdater(themeFolder, context));
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
        shutdownExecutorService();
        for (Command shutdownCommand : shutdownCommands) {
            try {
                shutdownCommand.execute();
            } catch (Exception e) {
                getLogger().error("Failed to execute shut down command {}",
                        shutdownCommand.getClass().getName(), e);
            }
        }
        shutdownCommands.clear();

    }

    @Override
    public void launchBrowserInDevelopmentMode(String url) {
        browserLauncher.launchBrowserInDevelopmentMode(url);
    }

    @Override
    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
        reportApplicationUrl();
    }

    private void setFullyStarted(boolean fullyStarted) {
        this.fullyStarted = fullyStarted;
        reportApplicationUrl();
    }

    private void reportApplicationUrl() {
        if (fullyStarted && applicationUrl != null) {
            getLogger().info("Application running at {}", applicationUrl);
        }
    }

    private void setDevModeStarted(VaadinContext context) {
        context.setAttribute(DevModeHandlerAlreadyStartedAttribute.class,
                new DevModeHandlerAlreadyStartedAttribute());
    }

    private void registerWatcherShutdownCommand(Closeable watcher) {
        registerShutdownCommand(() -> {
            try {
                watcher.close();
            } catch (Exception e) {
                getLogger().error("Failed to stop watcher {}",
                        watcher.getClass().getName(), e);
            }
        });
    }

    @Override
    public void registerShutdownCommand(Command command) {
        shutdownCommands.add(command);
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

    private void startWatchingPublicResourcesCss(VaadinContext context,
            ApplicationConfiguration config) {
        if (config.getMode() != Mode.DEVELOPMENT_BUNDLE) {
            // In dev-server (Vite) mode or production, static files are handled
            // elsewhere
            return;
        }
        try {
            File resourcesFolder = new File(
                    "src/main/resources/META-INF/resources");
            if (!resourcesFolder.isDirectory()) {
                getLogger().debug("No public resources folder found at {}",
                        resourcesFolder);
                return;
            }
            registerWatcherShutdownCommand(new PublicResourcesCssLiveUpdater(
                    resourcesFolder, context));
        } catch (Exception e) {
            getLogger().error(
                    "Failed to start live-reload for public CSS resources", e);
        }
    }
}
