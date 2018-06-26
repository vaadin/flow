package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.io.Serializable;

public class PwaConfiguration implements Serializable {
    public static final String DEFAULT_VERSION = "1.0";
    public static final String DEFAULT_PATH = "manifest.json";
    public static final String DEFAULT_LOGO = "icons/logo.png";
    public static final String DEFAULT_NAME = "Vaadin Flow Application";
    public static final String DEFAULT_THEME_COLOR =  "#ffffff";
    public static final String DEFAULT_BACKGROUND_COLOR = "#ffffff";
    public static final String DEFAULT_DISPLAY = "standalone";
    public static final String DEFAULT_START_URL = "/";
    public static final String DEFAULT_OFFLINE_PATH = "offline.html";

    private final String version;
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

    public PwaConfiguration(PWA pwa, ServletContext servletContext) {
        if (pwa != null) {
            this.version = pwa.version();
            this.appName = pwa.name();
            this.shortName = pwa.name();
            this.description = "";
            this.backgroundColor = pwa.backgroundColor();
            this.themeColor = pwa.themeColor();
            this.logoPath = checkPath(pwa.logoPath());
            this.manifestPath = checkPath(pwa.manifestPath());
            this.offlinePath = checkPath(pwa.offlinePath());
            this.display = pwa.display();
            this.startUrl = getStartUrl(servletContext);
            this.serviceWorkerPath = "sw.js";
            this.enabled = pwa.enabled();
        } else {
            this.version = DEFAULT_VERSION;
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
            this.enabled = false;
        }
    }

    public String getVersion() {
        return version;
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

    public boolean isEnabled() {
        return enabled;
    }

    private static String getStartUrl(ServletContext context) {
        return context == null || context.getContextPath() == null ||
                context.getContextPath().isEmpty() ? "/" :
                context.getContextPath();
    }

    private static String checkPath(String path) {
        return path.replaceAll("^[\\./]+", "");
    }


}
