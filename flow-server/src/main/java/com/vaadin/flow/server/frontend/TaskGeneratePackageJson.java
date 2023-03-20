/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.vaadin.experimental.FeatureFlags;

import elemental.json.JsonObject;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

/**
 * Creates the <code>package.json</code> if missing.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskGeneratePackageJson extends NodeUpdater {

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param npmFolder
     *            folder with the `package.json` file.
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param flowResourcesPath
     *            folder where flow resources taken from jars will be placed.
     *            default)
     * @param buildDir
     *            the used build directory
     */
    TaskGeneratePackageJson(File npmFolder, File generatedPath,
            File flowResourcesPath, String buildDir,
            FeatureFlags featureFlags) {
        super(null, null, npmFolder, generatedPath, flowResourcesPath, buildDir,
                featureFlags);
    }

    @Override
    public void execute() {
        try {
            modified = false;
            JsonObject mainContent = getPackageJson();
            modified = updateDefaultDependencies(mainContent);
            if (modified) {
                writePackageFile(mainContent);
            }

            if (flowResourcesFolder == null) {
                return;
            }

            if (!new File(npmFolder, NODE_MODULES + FLOW_NPM_PACKAGE_NAME)
                    .equals(flowResourcesFolder)) {
                writeResourcesPackageFile(getResourcesPackageJson());
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
