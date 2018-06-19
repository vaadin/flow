package com.vaadin.flow.server;

import javax.servlet.ServletContext;

public class PwaConfiguration {
    public static final String DEFAULT_PATH = "manifest.json";
    public static final String DEFAULT_LOGO = "icons/logo.png";
    public static final String DEFAULT_NAME = "Vaadin Flow Application";
    public static final String DEFAULT_THEME_COLOR =  "#ffffff";
    public static final String DEFAULT_BACKGROUND_COLOR = "#ffffff";
    public static final String DEFAULT_DISPLAY = "standalone";
    public static final String DEFAULT_START_URL = "/";
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
    private final boolean manifestDisabled;
    private final boolean serviceWorkerDisabled;

    public PwaConfiguration(Manifest manifest, ServletContext servletContext) {
        if (manifest != null) {
            this.appName = manifest.name();
            this.shortName = manifest.name();
            this.description = "";
            this.backgroundColor = manifest.backgroundColor();
            this.themeColor = manifest.themeColor();
            this.logoPath = checkPath(manifest.logoPath());
            this.manifestPath = checkPath(manifest.manifestPath());
            this.offlinePath = checkPath(manifest.offlinePath());
            this.display = manifest.display();
            this.startUrl = getStartUrl(servletContext);
            this.serviceWorkerPath = "sw.js";
            this.manifestDisabled = manifest.disableManifest();
            this.serviceWorkerDisabled = manifest.disableServiceWorker();
        } else {
            this.appName = DEFAULT_NAME;
            this.shortName = DEFAULT_NAME;
            this.description = "";
            this.backgroundColor = DEFAULT_BACKGROUND_COLOR;
            this.themeColor = DEFAULT_THEME_COLOR;
            this.logoPath = DEFAULT_LOGO;
            this.manifestPath = DEFAULT_PATH;
            this.offlinePath = DEFAULT_OFFLINE_PATH;
            this.display = DEFAULT_DISPLAY;
            this.startUrl = getStartUrl(servletContext);
            this.serviceWorkerPath = "sw.js";
            this.manifestDisabled = false;
            this.serviceWorkerDisabled = false;
        }
    }

    public String getAppName() {
        return appName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public String relLogoPath() {
        return "/" + logoPath;
    }

    public String getManifestPath() {
        return manifestPath;
    }

    public String relManifestPath() {
        return "/" + manifestPath;
    }

    public String getOfflinePath() {
        return offlinePath;
    }

    public String relOfflinePath() {
        return "/" + offlinePath;
    }

    public String getServiceWorkerPath() {
        return serviceWorkerPath;
    }

    public String relServiceWorkerPath() {
        return "/" + serviceWorkerPath;
    }


    /**
     * Defines the developers’ preferred display mode for the website.
     *
     * Possible values:
     * fullscreen, standalone, minimal-ui, browser
     *
     * @return
     */
    public String getDisplay() {
        return display;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public boolean isManifestDisabled() {
        return manifestDisabled;
    }

    public boolean isServiceWorkerDisabled() {
        return serviceWorkerDisabled;
    }

    private static String getStartUrl(ServletContext context) {
        return context.getContextPath().isEmpty() ? "/" :
                context.getContextPath();
    }

    private static String checkPath(String path) {
        return path.replaceAll("^[\\./]+", "");
    }


}
