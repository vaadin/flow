/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.plugin.base;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;

/**
 * The {@link PluginAdapterBase} with default methods, where possible.
 *
 */
public interface DefaultPluginAdapterBase extends PluginAdapterBase {

    @Override
    default File applicationProperties() {

        return projectBuildResourcesDirectory()
                .resolve("application.properties").toFile();
    }

    /**
     * Segment to buildOutput, used in default implementation of
     * `projectBuildOutputDirectory` to resolve from buildOutput from
     * projectBaseDirectory.
     *
     * @return {@link Path} Segment to buildOutput Folder
     *
     */
    default Path buildOutputPathSegment() {

        return buildPathSegment().resolve("classes");
    }

    /**
     * Segment to buildPath, used in default implementation of
     * `projectBuildDirectory` to resolve from buildPathSegment from
     * buildBaseDirectory.
     *
     * @return {@link Path} Segment to buildPath Folder
     *
     */
    default Path buildPathSegment() {

        return Paths.get("target");
    }

    /**
     * Segment to buildResources, used in default implementation of
     * `projectBuildResourcesDirectory` to resolve buildResources from
     * projectBaseDirectory.
     *
     * @return {@link Path} Segment to buildResources Folder
     *
     */
    default Path buildResourcesPathSegment() {

        return Paths.get("src/main/resources");
    }

    @Override
    default boolean eagerServerLoad() {

        return false;
    }

    @Override
    default File frontendDirectory() {

        return projectBaseDirectory().resolve(FRONTEND).toFile();
    }

    @Override
    default File generatedFolder() {

        return projectBuildDirectory().resolve(FRONTEND).toFile();
    }

    @Override
    default File generatedTsFolder() {

        return projectBaseDirectory().resolve(generatedTsPathSegment())
                .toFile();
    }

    /**
     * Segment to generatedTs, used in default implementation of
     * `generatedTsFolder` to resolve generatedTsFolder from
     * projectBaseDirectory.
     *
     * @return {@link Path} Segment to generatedTs
     *
     */
    default Path generatedTsPathSegment() {

        return Paths.get(FrontendUtils.FRONTEND, "generated");
    }

    @Override
    default File javaSourceFolder() {

        return projectBaseDirectory().resolve(javaSourceFolderPathSegment())
                .toFile();
    }

    /**
     * Segment to javaSourceFolder, used in default implementation of
     * `javaSourceFolder`to resolve javaSourceFolder from projectBaseDirectory.
     *
     * @return {@link Path} Segment to generatedTs
     *
     */

    default Path javaSourceFolderPathSegment() {

        return Paths.get("src/main/java");
    }

    @Override
    default String nodeDownloadRoot() {

        return NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT;
    }

    @Override
    default String nodeVersion() {

        return FrontendTools.DEFAULT_NODE_VERSION;
    }

    @Override
    default File npmFolder() {

        return projectBaseDirectory().toFile();
    }

    @Override
    default File openApiJsonFile() {

        return projectBuildDirectory()
                .resolve("generated-resources/openapi.json").toFile();
    }

    @Override
    default boolean pnpmEnable() {

        return Boolean.valueOf(Constants.ENABLE_PNPM_DEFAULT_STRING);
    }

    @Override
    default boolean productionMode() {

        return false;
    }

    @Override
    default Path projectBuildDirectory() {

        return projectBaseDirectory().resolve(buildPathSegment());
    }

    @Override
    default Path projectBuildOutputDirectory() {

        return projectBaseDirectory().resolve(buildOutputPathSegment());
    }

    @Override
    default Path projectBuildResourcesDirectory() {

        return projectBaseDirectory().resolve(buildResourcesPathSegment());
    }

    @Override
    default boolean requireHomeNodeExec() {

        return false;
    }

    @Override
    default File servletResourceOutputDirectory() {

        return projectBuildOutputDirectory().resolve(VAADIN_SERVLET_RESOURCES)
                .toFile();
    }

    @Override
    default File webpackOutputDirectory() {

        return projectBuildOutputDirectory().resolve(VAADIN_WEBAPP_RESOURCES)
                .toFile();
    }

}
