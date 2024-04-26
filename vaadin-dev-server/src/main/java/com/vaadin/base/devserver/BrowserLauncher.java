package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.open.Open;

import static com.vaadin.flow.server.InitParameters.LAUNCH_BROWSER_DELAY;

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

    private boolean isLaunched() {
        File launchFile = getLaunchFile();
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        int lastModifiedDelay = Integer.parseInt(applicationConfiguration
                .getStringProperty(LAUNCH_BROWSER_DELAY, "30"));
        // If launch file exists and is younger than time update file content
        // and modified stamp
        if (launchFile.exists()
                && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()
                        - launchFile.lastModified()) < lastModifiedDelay) {
            try {
                Files.writeString(launchFile.toPath(),
                        "" + System.currentTimeMillis());
            } catch (IOException e) {
                LoggerFactory.getLogger(BrowserLauncher.class)
                        .debug("Failed to write browser launched file.", e);
            }
            return true;
        }
        return LAUNCHED_VALUE.equals(System.getProperty(LAUNCH_TRACKER));
    }

    private File getLaunchFile() {
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        File buildFolder = new File(applicationConfiguration.getProjectFolder(),
                applicationConfiguration.getBuildFolder());
        return new File(buildFolder, "tab.launch");
    }

    private void setLaunched() {
        // write launch file and update modified timestamp.
        try {
            Files.writeString(getLaunchFile().toPath(),
                    "" + System.currentTimeMillis());
        } catch (IOException e) {
            LoggerFactory.getLogger(BrowserLauncher.class)
                    .debug("Failed to write browser launched file.", e);
        }
        System.setProperty(LAUNCH_TRACKER, LAUNCHED_VALUE);
    }

}
