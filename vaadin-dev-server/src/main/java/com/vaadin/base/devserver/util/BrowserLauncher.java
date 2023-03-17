/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for launching a browser.
 */
public class BrowserLauncher {

    private static File bundleTempFile;

    private BrowserLauncher() {
        // Static methods only
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BrowserLauncher.class);
    }

    private static int runNodeCommands(String script)
            throws InterruptedException, IOException {
        FrontendToolsSettings settings = new FrontendToolsSettings("",
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        FrontendTools tools = new FrontendTools(settings);
        String node = tools.getNodeExecutable();
        List<String> command = new ArrayList<>();
        command.add(node);
        command.add("-e");
        command.add(script);
        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        return builder.start().waitFor();
    }

    private static File getBundleFile() {
        if (bundleTempFile != null) {
            return bundleTempFile;
        }

        try {
            bundleTempFile = File.createTempFile("vaadin-dev-server-bundle",
                    "js");
            bundleTempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(bundleTempFile)) {
                IOUtils.copyLarge(BrowserLauncher.class.getResourceAsStream(
                        "/vaadin-dev-server-node-bundle.js"), out);
            }
            return bundleTempFile;
        } catch (Exception e) {
            getLogger().error("Unable to create temp file for bundle", e);
        }

        return null;
    }

    /**
     * Launch a local browser using the given location URL.
     * 
     * @param location
     *            the URL to open
     */
    public static void launch(String location) {
        launch(location, "Unable to open " + location + " in a browser");
    }

    /**
     * Launch a local browser using the given location URL.
     * <p>
     * If opening a browser fails, log the given text instead.
     * 
     * @param location
     *            the URL to open
     * @param alternativeText
     *            the text to show if launching fails
     */
    public static void launch(String location, String alternativeText) {
        try {
            File bundleFile = getBundleFile();
            if (bundleFile == null) {
                // Error should have been logged already
                return;
            }
            String bundleFilePath = bundleFile.getAbsolutePath().replace("\\",
                    "/");
            String cmd = String.format(
                    "const open = require('%s').open;open('%s');",
                    bundleFilePath, location.replace("'", "\\'"));
            runNodeCommands(cmd);
        } catch (Exception e) {
            getLogger().debug("Unable to launch browser", e);
            getLogger().info(alternativeText);
        }
    }

}
