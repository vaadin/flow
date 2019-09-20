/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;

/**
 * Holds the configuration from the {@link PWA} annotation.
 *
 * Takes {@link PWA} in constructor to fill properties. Sanitizes the input and
 * falls back to default values if {@link PWA} is unavailable ({@code null}).
 *
 * @since 1.2
 */
public class PwaConfiguration implements Serializable {
    public static final String DEFAULT_PATH = "manifest.webmanifest";
    public static final String DEFAULT_ICON = "icons/icon.png";
    public static final String DEFAULT_NAME = "Vaadin Flow Application";
    public static final String DEFAULT_THEME_COLOR = "#ffffff";
    public static final String DEFAULT_BACKGROUND_COLOR = "#f2f2f2";
    public static final String DEFAULT_DISPLAY = "standalone";
    public static final String DEFAULT_OFFLINE_PATH = "offline.html";

    private final String appName;
    private final String shortName;
    private final String description;
    private final String backgroundColor;
    private final String themeColor;
    private final String iconPath;
    private final String manifestPath;
    private final String offlinePath;
    private final String serviceWorkerPath = "sw.js";
    private final String display;
    private final String rootUrl;
    private final String startPath;
    private final boolean enabled;
    private final List<String> offlineResources;
    private final boolean enableInstallPrompt;

    protected PwaConfiguration(PWA pwa, ServletContext servletContext) {
        rootUrl = hasContextPath(servletContext)
                ? servletContext.getContextPath() + "/"
                : "/";
        if (pwa != null) {
            appName = pwa.name();
            shortName = pwa.shortName().substring(0,
                    Math.min(pwa.shortName().length(), 12));
            description = pwa.description();
            backgroundColor = pwa.backgroundColor();
            themeColor = pwa.themeColor();
            iconPath = checkPath(pwa.iconPath());
            manifestPath = checkPath(pwa.manifestPath());
            offlinePath = checkPath(pwa.offlinePath());
            display = pwa.display();
            startPath = pwa.startPath().replaceAll("^/+", "");
            enabled = true;
            offlineResources = Arrays.asList(pwa.offlineResources());
            enableInstallPrompt = pwa.enableInstallPrompt();
        } else {
            appName = DEFAULT_NAME;
            shortName = "Flow PWA";
            description = "";
            backgroundColor = DEFAULT_BACKGROUND_COLOR;
            themeColor = DEFAULT_THEME_COLOR;
            iconPath = DEFAULT_ICON;
            manifestPath = DEFAULT_PATH;
            offlinePath = DEFAULT_OFFLINE_PATH;
            display = DEFAULT_DISPLAY;
            startPath = "";
            enabled = false;
            offlineResources = Collections.emptyList();
            enableInstallPrompt = false;
        }
    }

    private static boolean hasContextPath(ServletContext servletContext) {
        return !(servletContext == null
                || servletContext.getContextPath() == null
                || servletContext.getContextPath().isEmpty());
    }

    private static String checkPath(String path) {
        return path.replaceAll("^[./]+", "");
    }

    /**
     * Gets the application name.
     *
     * @return application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Gets the application short name.
     *
     * Max 12 characters.
     *
     * @return application short name
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Gets the application description.
     *
     * @return application description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the background color of the application.
     *
     * The background color property is used on the splash screen when the
     * application is first launched.
     *
     * @return background color of the application
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Gets the theme color of the application.
     *
     * The theme color sets the color of the application's tool bar and
     * application's color in the task switcher.
     *
     * @return theme color of the application
     */
    public String getThemeColor() {
        return themeColor;
    }

    /**
     * Gets the path to the application icon file.
     * <p>
     * Example: {@literal img/my-icon.png}
     *
     * @return path to the application icon file
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Gets the ath to icon with prefix, so request matches.
     *
     * @return path to icon with prefix, so request matches
     */
    public String relIconPath() {
        return "/" + iconPath;
    }

    /**
     * Gets the path to the manifest.webmanifest.
     *
     * @return path to the manifest.webmanifest
     */
    public String getManifestPath() {
        return manifestPath;
    }

    /**
     * Path to manifest with prefix, so request matches.
     *
     * @return path to manifest with prefix, so request matches
     */
    public String relManifestPath() {
        return "/" + manifestPath;
    }

    /**
     * Path to static offline html file.
     *
     * @return path to static offline html file
     */
    public String getOfflinePath() {
        return offlinePath;
    }

    /**
     * Path to offline file with prefix, so request matches.
     *
     * @return path to offline file with prefix, so request matches
     */
    public String relOfflinePath() {
        return "/" + offlinePath;
    }

    /**
     * Gets the path to the service worker.
     *
     * @return path to service worker
     */
    public String getServiceWorkerPath() {
        return serviceWorkerPath;
    }

    /**
     * Gets the path to service worker with prefix, so request matches.
     *
     * @return path to service worker with prefix, so request matches
     */
    public String relServiceWorkerPath() {
        return "/" + serviceWorkerPath;
    }

    /**
     * Gets the list of files to be added to pre cache.
     *
     * @return list of files to be added to pre cache
     */
    public List<String> getOfflineResources() {
        return Collections.unmodifiableList(offlineResources);
    }

    /**
     * Gets the the developers’ preferred display mode for the website.
     *
     * Possible values: fullscreen, standalone, minimal-ui, browser
     *
     * @return display mode
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Gets the start url of the PWA application.
     *
     * <p>
     * Used in manifest as start url.
     *
     * @return start url of the PWA application
     */
    public String getStartUrl() {
        return rootUrl + startPath;
    }

    /**
     * Gets the application root url.
     *
     * @return application root url
     */
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Is PWA enabled.
     *
     * @return is PWA enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Is install prompt resources injection enabled.
     * <p>
     * If enabled, server will inject required html and js to bootstrap page.
     *
     * @return is install prompt resources injection enabled
     */
    public boolean isInstallPromptEnabled() {
        return enableInstallPrompt;
    }
}
