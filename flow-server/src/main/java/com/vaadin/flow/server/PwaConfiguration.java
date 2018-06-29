/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class implementation of {@link PWA} annotation.
 *
 * Takes {@link PWA} in constructor to fill properties.
 * Sanitizes the input and falls back to default values if {@link PWA} is
 * unavailable (null).
 *
 */
public class PwaConfiguration implements Serializable {
    public static final String DEFAULT_PATH = "manifest.json";
    public static final String DEFAULT_LOGO = "icons/logo.png";
    public static final String DEFAULT_NAME = "Vaadin Flow Application";
    public static final String DEFAULT_THEME_COLOR =  "#ffffff";
    public static final String DEFAULT_BACKGROUND_COLOR = "#f2f2f2";
    public static final String DEFAULT_DISPLAY = "fullscreen";
    public static final String DEFAULT_OFFLINE_PATH = "offline.html";

    private final String appName;
    private final String shortName;
    private final String description;
    private final String backgroundColor;
    private final String themeColor;
    private final String logoPath;
    private final String manifestPath;
    private final String offlinePath;
    private final String serviceWorkerPath;
    private final String display;
    private final String startUrl;
    private final boolean enabled;
    private List<String> offlineResources;

    protected PwaConfiguration(PWA pwa, ServletContext servletContext) {
        if (pwa != null) {
            this.appName = pwa.name();
            this.shortName = pwa.shortName().substring(0,
                    Math.min(pwa.shortName().length(), 12));
            this.description = pwa.description();
            this.backgroundColor = pwa.backgroundColor();
            this.themeColor = pwa.themeColor();
            this.logoPath = checkPath(pwa.logoPath());
            this.manifestPath = checkPath(pwa.manifestPath());
            this.offlinePath = checkPath(pwa.offlinePath());
            this.display = pwa.display();
            this.startUrl = getStartUrl(servletContext);
            this.serviceWorkerPath = "sw.js";
            this.enabled = pwa.enabled();
            this.offlineResources = Arrays.asList(pwa.offlineResources());
        } else {
            this.appName = DEFAULT_NAME;
            this.shortName = "Flow PWA";
            this.description = "";
            this.backgroundColor = DEFAULT_BACKGROUND_COLOR;
            this.themeColor = DEFAULT_THEME_COLOR;
            this.logoPath = DEFAULT_LOGO;
            this.manifestPath = DEFAULT_PATH;
            this.offlinePath = DEFAULT_OFFLINE_PATH;
            this.display = DEFAULT_DISPLAY;
            this.startUrl = getStartUrl(servletContext);
            this.serviceWorkerPath = "sw.js";
            this.enabled = false;
            this.offlineResources = Collections.emptyList();
        }
    }

    /**
     * Application name.
     *
     * @return Application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Application short name.
     *
     * Max 12 characters.
     *
     * @return Application short name
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Application description.
     *
     * @return Application description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Background color of application.
     *
     * The background_color property is used on the splash screen when the
     * application is first launched.
     *
     * @return Background color of application
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Theme color of application.
     *
     * The theme color sets the color of the tool bar, and in the task switcher.
     *
     * @return Theme color of application
     */
    public String getThemeColor() {
        return themeColor;
    }

    /**
     * Path to logo.
     *
     * For example "img/my-icon.png"
     *
     * @return Path to logo
     */
    public String getLogoPath() {
        return logoPath;
    }

    /**
     * Path to logo with prefix, so request matches.
     *
     * @return Path to logo with prefix, so request matches
     */
    public String relLogoPath() {
        return "/" + logoPath;
    }

    /**
     * Path to manifest.json.
     *
     * @return Path to manifest.json
     */
    public String getManifestPath() {
        return manifestPath;
    }

    /**
     * Path to manifest with prefix, so request matches.
     *
     * @return Path to manifest with prefix, so request matches
     */
    public String relManifestPath() {
        return "/" + manifestPath;
    }

    /**
     * Path to static offline html file.
     *
     * @return Path to static offline html file
     */
    public String getOfflinePath() {
        return offlinePath;
    }

    /**
     * Path to offline file with prefix, so request matches.
     *
     * @return Path to offline file with prefix, so request matches
     */
    public String relOfflinePath() {
        return "/" + offlinePath;
    }

    /**
     * Path to service worker.
     *
     * @return Path to service worker
     */
    public String getServiceWorkerPath() {
        return serviceWorkerPath;
    }

    /**
     * Path to service worker with prefix, so request matches.
     *
     * @return Path to service worker with prefix, so request matches
     */
    public String relServiceWorkerPath() {
        return "/" + serviceWorkerPath;
    }

    /**
     * List of files to be added to pre cache.
     *
     * @return List of files to be added to pre cache
     */
    public List<String> getOfflineResources() {
        return Collections.unmodifiableList(offlineResources);
    }

    /**
     * Defines the developers’ preferred display mode for the website.
     *
     * Possible values:
     * fullscreen, standalone, minimal-ui, browser
     *
     * @return display mode
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Start url of the PWA application.
     *
     * @return Start url of the PWA application
     */
    public String getStartUrl() {
        return startUrl;
    }

    /**
     * Is PWA enabled.
     *
     * @return Is PWA enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    private static String getStartUrl(ServletContext context) {
        return context == null || context.getContextPath() == null ||
                context.getContextPath().isEmpty() ? "/" :
                context.getContextPath() + "/";
    }

    private static String checkPath(String path) {
        return path.replaceAll("^[\\./]+", "");
    }


}
