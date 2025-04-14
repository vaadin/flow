/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;

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
    private final String rootUrl;
    private final String startPath;
    private final boolean enabled;
    private final List<String> offlineResources;
    private final boolean enableInstallPrompt;

    /**
     * Creates the configuration using default PWA parameters.
     */
    public PwaConfiguration() {
        this(false, "/", DEFAULT_NAME, "Flow PWA", "", DEFAULT_BACKGROUND_COLOR,
                DEFAULT_THEME_COLOR, DEFAULT_ICON, DEFAULT_PATH,
                DEFAULT_OFFLINE_PATH, DEFAULT_DISPLAY, "", new String[0],
                false);
    }

    protected PwaConfiguration(PWA pwa, ServletContext servletContext) {
        this(true, rootUrlPath(servletContext), pwa.name(),
                pwa.shortName().substring(0,
                        Math.min(pwa.shortName().length(), 12)),
                pwa.description(), pwa.backgroundColor(), pwa.themeColor(),
                checkPath(pwa.iconPath()), checkPath(pwa.manifestPath()),
                checkPath(pwa.offlinePath()), pwa.display(),
                pwa.startPath().replaceAll("^/+", ""), pwa.offlineResources(),
                pwa.enableInstallPrompt());
    }

    /**
     * Constructs a configuration from individual values.
     *
     * @param enabled
     *            is PWA enabled
     * @param name
     *            the application name
     * @param shortName
     *            the application short name
     * @param description
     *            the description of the application
     * @param backgroundColor
     *            the background color
     * @param themeColor
     *            the theme color
     * @param iconPath
     *            the icon file path
     * @param manifestPath
     *            the `manifest.webmanifest` file path
     * @param offlinePath
     *            the static offline HTML file path
     * @param display
     *            the display mode
     * @param startPath
     *            the start path
     * @param offlineResources
     *            the list of files to add for pre-caching
     * @param enableInstallPrompt
     *            is install prompt resources injection enabled.
     */
    public PwaConfiguration(boolean enabled, String rootUrl, String name,
            String shortName, String description, String backgroundColor,
            String themeColor, String iconPath, String manifestPath,
            String offlinePath, String display, String startPath,
            String[] offlineResources, boolean enableInstallPrompt) {
        this.rootUrl = rootUrl;
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
        this.enableInstallPrompt = enableInstallPrompt;
    }

    private static String rootUrlPath(ServletContext servletContext) {
        return hasContextPath(servletContext)
                ? servletContext.getContextPath() + "/"
                : "/";
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
