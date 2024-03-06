/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.PwaConfiguration;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_FILES_PATH;
import static elemental.json.impl.JsonUtil.stringify;

/**
 * Creates a vaadin-dev-server-settings.json file for use with dev server
 * configuration.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskUpdateSettingsFile implements FallibleCommand, Serializable {

    public static final String DEV_SETTINGS_FILE = "vaadin-dev-server-settings.json";
    File npmFolder;
    File frontendDirectory;
    File jarFrontendResourcesFolder;
    File webappResourcesDirectory;
    String buildDirectory;
    String themeName;
    PwaConfiguration pwaConfiguration;

    TaskUpdateSettingsFile(Options builder, String themeName,
            PwaConfiguration pwaConfiguration) {
        this.npmFolder = builder.getNpmFolder();
        this.frontendDirectory = builder.getFrontendDirectory();
        this.jarFrontendResourcesFolder = builder
                .getJarFrontendResourcesFolder();
        this.webappResourcesDirectory = builder.getWebappResourcesDirectory();
        this.buildDirectory = builder.getBuildDirectory();
        this.themeName = themeName;
        this.pwaConfiguration = pwaConfiguration;
    }

    @Override
    public void execute() {
        if (npmFolder == null)
            return;

        JsonObject settings = Json.createObject();
        settings.put("frontendFolder",
                FrontendUtils.getUnixPath(frontendDirectory.toPath()));
        settings.put("themeFolder", "themes");
        settings.put("themeResourceFolder",
                FrontendUtils.getUnixPath(jarFrontendResourcesFolder.toPath()));
        String webappResources, statsOutput;
        if (webappResourcesDirectory == null) {
            webappResources = combinePath(buildDirectory, "classes",
                    VAADIN_WEBAPP_RESOURCES);
            statsOutput = combinePath(buildDirectory, "classes",
                    VAADIN_WEBAPP_RESOURCES, "..", "config");
        } else {
            webappResources = webappResourcesDirectory.getPath();
            statsOutput = new File(webappResourcesDirectory.getParentFile(),
                    "config").getPath();
        }
        String staticOutput = combinePath(webappResources,
                VAADIN_STATIC_FILES_PATH);

        settings.put("staticOutput",
                FrontendUtils.getUnixPath(new File(staticOutput).toPath()));
        settings.put("generatedFolder", "generated");
        settings.put("statsOutput", statsOutput);
        settings.put("frontendBundleOutput", webappResources);
        settings.put("jarResourcesFolder",
                FrontendUtils.getUnixPath(jarFrontendResourcesFolder.toPath()));
        settings.put("generatedFlowImportsFolder",
                buildDirectory + "/frontend");

        settings.put("themeName", themeName);

        settings.put("clientServiceWorkerSource", getServiceWorkerFile());

        settings.put("pwaEnabled", pwaConfiguration.isEnabled());

        settings.put("offlineEnabled", pwaConfiguration.isOfflineEnabled());

        settings.put("offlinePath", getOfflinePath());

        File settingsFile = new File(npmFolder,
                buildDirectory + "/" + DEV_SETTINGS_FILE);

        try {
            FileUtils.write(settingsFile, stringify(settings, 2),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log().error("Failed to write file: {}", settingsFile, e);
        }
    }

    private String combinePath(String... parts) {
        StringBuilder path = new StringBuilder();
        for (String part : parts) {
            path.append(part);
            if (!part.endsWith("/")) {
                path.append('/');
            }
        }
        return path.toString();
    }

    private String getServiceWorkerFile() {
        boolean exists = new File(frontendDirectory, SERVICE_WORKER_SRC)
                .exists()
                || new File(frontendDirectory, SERVICE_WORKER_SRC_JS).exists();

        String serviceWorkerFile = SERVICE_WORKER_SRC;
        if (new File(frontendDirectory, SERVICE_WORKER_SRC_JS).exists()) {
            serviceWorkerFile = SERVICE_WORKER_SRC_JS;
        }

        if (!exists) {
            Path path = Paths.get(npmFolder.toString(), buildDirectory,
                    serviceWorkerFile);
            return path.toString();
        } else {
            return Paths.get(frontendDirectory.toString(), serviceWorkerFile)
                    .toString();
        }
    }

    private String getOfflinePath() {
        if (pwaConfiguration.isOfflinePathEnabled()) {
            return "'" + getEscapedRelativePath(
                    Paths.get(pwaConfiguration.getOfflinePath())) + "'";
        }
        return "'.'";
    }

    private String getEscapedRelativePath(Path path) {
        if (path.isAbsolute()) {
            return FrontendUtils.getUnixRelativePath(npmFolder.toPath(), path);
        } else {
            return FrontendUtils.getUnixPath(path);
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
