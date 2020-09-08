/*
 * Copyright 2000-2020 Vaadin Ltd.
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
    private final String startPath;
    private final boolean enabled;
    private final List<String> offlineResources;

    /**
     * Default constructor, uses default values.
     */
    public PwaConfiguration() {
        this(false, DEFAULT_NAME, "Flow PWA", "", DEFAULT_BACKGROUND_COLOR,
                DEFAULT_THEME_COLOR, DEFAULT_ICON, DEFAULT_PATH,
                DEFAULT_OFFLINE_PATH, DEFAULT_DISPLAY, "", new String[] {});
    }

    /**
     * Constructs the configuration using the {@link PWA} annotation.
     *
     * @param pwa the annotation to use for configuration
     */
    public PwaConfiguration(PWA pwa) {
        this(true, pwa.name(), pwa.shortName(), pwa.description(),
                pwa.backgroundColor(), pwa.themeColor(), pwa.iconPath(),
                pwa.manifestPath(), pwa.offlinePath(), pwa.display(),
                pwa.startPath(), pwa.offlineResources());
    }

    /**
     * Constructs a configuration from individual values.
     *
     * @param enabled is PWA enabled
     * @param name the application name
     * @param shortName the application short name
     * @param description the description of the application
     * @param backgroundColor the background color
     * @param themeColor the theme color
     * @param iconPath the icon file path
     * @param manifestPath the `manifest.webmanifest` file path
     * @param offlinePath the static offline HTML file path
     * @param display the display mode
     * @param startPath the start path
     * @param offlineResources the list of files to add for pre-caching
     */
    @SuppressWarnings("squid:S00107")
    public PwaConfiguration(boolean enabled, String name, String shortName,
            String description, String backgroundColor, String themeColor,
            String iconPath, String manifestPath, String offlinePath,
            String display, String startPath, String[] offlineResources) {
        this.appName = name;
        this.shortName = shortName.substring(0,
                Math.min(shortName.length(), 12));
        this.description = description;
        this.backgroundColor = backgroundColor;
        this.themeColor = themeColor;
        this.iconPath = iconPath;
        this.manifestPath = manifestPath;
        this.offlinePath = offlinePath;
        this.display = display;
        this.startPath = startPath;
        this.enabled = enabled;
        this.offlineResources = Arrays.asList(offlineResources);
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
        return startPath;
    }

    /**
     * Is PWA enabled.
     *
     * @return is PWA enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
