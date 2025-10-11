/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.server.frontend.BundleUtils;
import com.vaadin.flow.server.frontend.FileIOUtils;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION;

/**
 * Defines a base contract for configuration (e.g. on an application level,
 * servlet level,...).
 *
 * @author Vaadin Ltd
 *
 */
public interface AbstractConfiguration extends Serializable {
    /**
     * Returns whether Vaadin is in production mode.
     *
     * @return true if in production mode, false otherwise.
     */
    boolean isProductionMode();

    default File getFrontendFolder() {
        String frontendFolderPath = getStringProperty(
                FrontendUtils.PARAM_FRONTEND_DIR,
                FrontendUtils.DEFAULT_FRONTEND_DIR);

        File frontend = new File(frontendFolderPath);
        if (!frontend.isAbsolute()) {
            frontend = new File(getProjectFolder(), frontendFolderPath);
        }

        return FrontendUtils.getFrontendFolder(getProjectFolder(), frontend);
    }

    /**
     * Gets the mode the application is running in.
     *
     * @return custom production bundle, pre-compiled production bundle,
     *         development using livereload or development using bundle
     **/
    default Mode getMode() {
        if (isProductionMode()) {
            return BundleUtils.isPreCompiledProductionBundle()
                    ? Mode.PRODUCTION_PRECOMPILED_BUNDLE
                    : Mode.PRODUCTION_CUSTOM;
        } else if (getBooleanProperty(InitParameters.FRONTEND_HOTDEPLOY,
                FrontendUtils.isHillaUsed(getFrontendFolder()))) {
            return Mode.DEVELOPMENT_FRONTEND_LIVERELOAD;
        } else {
            return Mode.DEVELOPMENT_BUNDLE;
        }
    }

    /**
     * Get if the dev server should be reused on each reload. True by default,
     * set it to false in tests so as dev server is not kept as a daemon after
     * the test.
     *
     * @return true if dev server should be reused
     */
    default boolean reuseDevServer() {
        return getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER, true);
    }

    /**
     * Gets a configured property as a string.
     *
     * @param name
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     */
    String getStringProperty(String name, String defaultValue);

    /**
     * Gets a configured property as a boolean.
     *
     *
     * @param name
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     *
     */
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Returns whether pnpm is enabled or not.
     *
     * @return {@code true} if enabled, {@code false} if not
     */
    default boolean isPnpmEnabled() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                Constants.ENABLE_PNPM_DEFAULT);
    }

    /**
     * Returns whether bun is enabled or not.
     *
     * @return {@code true} if enabled, {@code false} if not
     */
    default boolean isBunEnabled() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_ENABLE_BUN,
                Constants.ENABLE_BUN_DEFAULT);
    }

    /**
     * Returns whether globally installed pnpm is used or the default one (see
     * {@link com.vaadin.flow.server.frontend.FrontendTools#DEFAULT_PNPM_VERSION}).
     *
     * @return {@code true} if globally installed pnpm is used, {@code false} if
     *         the default one is used.
     */
    default boolean isGlobalPnpm() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM,
                Constants.GLOBAL_PNPM_DEFAULT);
    }

    /**
     * Returns whether development time usage statistics collection is enabled
     * or not.
     *
     * Always return false if <code>isProductionMode</code> is {@code true}.
     *
     * @see #isProductionMode()
     * @return {@code true} if enabled, {@code false} if not collected.
     */
    default boolean isUsageStatisticsEnabled() {
        return !isProductionMode() && getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_STATISTICS,
                Constants.DEFAULT_DEVMODE_STATS);
    }

    /**
     * Returns whether cross-site request forgery protection is enabled.
     *
     * @return true if XSRF protection is enabled, false otherwise.
     */
    default boolean isXsrfProtectionEnabled() {
        return !getBooleanProperty(SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                false);
    }

    /**
     * Return the defined build folder for the used build system.
     * <p>
     * Default value is <code>target</code> used by maven and the gradle plugin
     * will set it to <code>build</code>.
     *
     * @return build folder name, default {@code target}
     */
    default String getBuildFolder() {
        return getStringProperty(InitParameters.BUILD_FOLDER, Constants.TARGET);
    }

    /**
     * Return the project root folder.
     * <p>
     * Only available in development mode.
     *
     * @return the project root folder, or {@code null} if unknown
     */
    default File getProjectFolder() {
        if (isProductionMode()) {
            return null;
        }

        String folder = getStringProperty(FrontendUtils.PROJECT_BASEDIR, null);
        if (folder != null) {
            return new File(folder);
        }

        File projectFolder = FileIOUtils.getProjectFolderFromClasspath();
        if (projectFolder != null) {
            return projectFolder;
        }

        /*
         * Accept user.dir or cwd as a fallback only if the directory seems to
         * be a Maven or Gradle project. Check to avoid cluttering server
         * directories (see tickets #8249, #8403).
         */
        String baseDirCandidate = System.getProperty("user.dir", ".");
        Path path = Paths.get(baseDirCandidate);
        if (path.toFile().isDirectory() && (path.resolve("pom.xml").toFile()
                .exists() || path.resolve("build.gradle").toFile().exists()
                || path.resolve("build.gradle.kts").toFile().exists())) {
            return path.toAbsolutePath().toFile();
        } else {
            throw new IllegalStateException(String.format(
                    "Failed to determine project directory for dev mode. "
                            + "Directory '%s' does not look like a Maven or "
                            + "Gradle project. Ensure that you have run the "
                            + "prepare-frontend Maven goal, which generates "
                            + "'flow-build-info.json', prior to deploying your "
                            + "application",
                    path.toString()));
        }
    }

    /**
     * Gets the folder where resource sources are stored.
     * <p>
     * Only available in development mode.
     *
     * @return the folder where resources are stored, typically
     *         {@code src/main/resources}.
     */
    default File getJavaResourceFolder() {
        File folder = new File(getStringProperty(
                Constants.JAVA_RESOURCE_FOLDER_TOKEN, "src/main/resources"));
        if (!folder.isAbsolute()) {
            folder = new File(getProjectFolder(), folder.getPath());
        }
        return folder.getAbsoluteFile();
    }

    /**
     * Gets the folder where sources are stored.
     * <p>
     * Only available in development mode.
     *
     * @return the folder where source files are stored, typically
     *         {@code src/main/java}.
     */
    default File getJavaSourceFolder() {
        File folder = new File(getStringProperty(
                Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN, "src/main/java"));
        if (!folder.isAbsolute()) {
            folder = new File(getProjectFolder(), folder.getPath());
        }
        return folder.getAbsoluteFile();
    }
}
