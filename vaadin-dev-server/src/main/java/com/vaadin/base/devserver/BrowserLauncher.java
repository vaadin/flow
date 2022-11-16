package com.vaadin.base.devserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.open.Open;

/**
 * Util for launching a browser instance.
 */
public class BrowserLauncher {

    private static final String LAUNCH_TRACKER = "LaunchUtil.hasLaunched";
    private static final String LAUNCHED_VALUE = "yes";

    private VaadinContext context;

    /**
     * Init a launcher for the given context.
     */
    public BrowserLauncher(VaadinContext context) {
        this.context = context;
    }

    /**
     * Open the given URL in the default browser.
     */
    public void launchBrowserInDevelopmentMode(String url) {
        if (isLaunched()) {
            // Only launch browser on startup, not on reload
            return;
        }
        if (!isProductionMode()) {
            String outputOnFailure = "Application started at " + url;
            if (!Open.open(url)) {
                getLogger().info(outputOnFailure);
            }
            setLaunched();
        }

    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    private boolean isProductionMode() {
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        return applicationConfiguration.isProductionMode();
    }

    private static boolean isLaunched() {
        return LAUNCHED_VALUE.equals(System.getProperty(LAUNCH_TRACKER));
    }

    private static void setLaunched() {
        System.setProperty(LAUNCH_TRACKER, LAUNCHED_VALUE);
    }

}
