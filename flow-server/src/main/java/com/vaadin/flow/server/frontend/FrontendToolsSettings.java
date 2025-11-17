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
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;

import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.frontend.FrontendTools.DEFAULT_NODE_VERSION;
import com.vaadin.flow.server.frontend.installer.Platform;

/**
 * Configuration object for controlling the {@link FrontendTools} features.
 * <p>
 * This can be modified, but the choices will be locked in {@link FrontendTools}
 * when it is initialized. Until then any settings can be changed.
 */
public class FrontendToolsSettings implements Serializable {

    private String baseDir;
    private SerializableSupplier<String> alternativeDirGetter;

    private String nodeVersion = DEFAULT_NODE_VERSION;
    private URI nodeDownloadRoot = URI
            .create(Platform.guess().getNodeDownloadRoot());

    private boolean ignoreVersionChecks;
    private boolean forceAlternativeNode = Constants.DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE;
    private boolean useGlobalPnpm = Constants.GLOBAL_PNPM_DEFAULT;
    private boolean autoUpdate = Constants.DEFAULT_NODE_AUTO_UPDATE;

    /**
     * Create a tools configuration object.
     * <p>
     * The {@code baseDir} is used as a base directory to locate the tools and
     * {@code alternativeDirGetter} is the directory to install tools if they
     * are not found.
     * <p>
     * Note that if {@code alternativeDir} is {@code null} tools won't be
     * installed.
     *
     * @param baseDir
     *            the base directory to locate the tools, not {@code null}
     * @param alternativeDirGetter
     *            the getter for a directory where tools will be installed if
     *            they are not found globally or in the {@code baseDir}, may be
     *            {@code null}
     */
    public FrontendToolsSettings(String baseDir,
            SerializableSupplier<String> alternativeDirGetter) {
        this.baseDir = Objects.requireNonNull(baseDir);
        this.alternativeDirGetter = alternativeDirGetter;
    }

    /**
     * Set the base directory for locating tools.
     *
     * @param baseDir
     *            the base directory to locate the tools, not {@code null}
     */
    public void setBaseDir(String baseDir) {
        this.baseDir = Objects.requireNonNull(baseDir);
    }

    /**
     * Set the installation directory if no tools are found.
     *
     * @param alternativeDirGetter
     *            the getter for a directory where tools will be installed if
     *            they are not found globally or in the {@code baseDir}, may be
     *            {@code null}
     */
    public void setAlternativeDirGetter(
            SerializableSupplier<String> alternativeDirGetter) {
        this.alternativeDirGetter = alternativeDirGetter;
    }

    /**
     * Set the root URI for downloading node.
     *
     * @param nodeDownloadRoot
     *            node download root uri, default is
     *            {@value NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}
     */
    public void setNodeDownloadRoot(URI nodeDownloadRoot) {
        this.nodeDownloadRoot = nodeDownloadRoot;
    }

    /**
     * Set the node version to install when installation is required.
     *
     * @param nodeVersion
     *            The node.js version to be used when node.js is installed
     *            automatically, default is
     *            {@value FrontendTools#DEFAULT_NODE_VERSION}
     */
    public void setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
    }

    /**
     * Set if node and npm versions should be checked or not.
     *
     * If set, system property
     * {@value FrontendUtils#PARAM_IGNORE_VERSION_CHECKS} will override the
     * value set here.
     *
     * @param ignoreVersionChecks
     *            set to {@code true} if versions should be validated
     */
    public void setIgnoreVersionChecks(boolean ignoreVersionChecks) {
        String val = System
                .getProperty(FrontendUtils.PARAM_IGNORE_VERSION_CHECKS);
        if (val == null) {
            this.ignoreVersionChecks = ignoreVersionChecks;
        } else {
            this.ignoreVersionChecks = Boolean
                    .getBoolean(FrontendUtils.PARAM_IGNORE_VERSION_CHECKS);
        }
    }

    /**
     * Set if the alternative folder should always be used even if a global
     * installation exists.
     * <p>
     * This will force the installation if a version doesn't exist in the folder
     * defined in {@link #alternativeDirGetter}.
     *
     * @param forceAlternativeNode
     *            if {@code true} force usage of node executable from
     *            alternative directory
     */
    public void setForceAlternativeNode(boolean forceAlternativeNode) {
        this.forceAlternativeNode = forceAlternativeNode;
    }

    /**
     * Force usage of global pnpm.
     *
     * @param useGlobalPnpm
     *            use globally installed pnpm instead of the default version
     *            {@value FrontendTools#DEFAULT_PNPM_VERSION}
     */
    public void setUseGlobalPnpm(boolean useGlobalPnpm) {
        this.useGlobalPnpm = useGlobalPnpm;
    }

    /**
     * When set to true the alternative version is updated to the latest default
     * node version as defined for the framework.
     *
     * @param autoUpdate
     *            update node in {@link #alternativeDirGetter} if version older
     *            than the current default
     *            {@value FrontendTools#DEFAULT_NODE_VERSION}
     */
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    /**
     * Get the defined base dir.
     *
     * @return defined base dir
     */
    public String getBaseDir() {
        return baseDir;
    }

    /**
     * Get the alternative directory getter.
     *
     * @return alternative directory getter
     */
    public Supplier<String> getAlternativeDirGetter() {
        return alternativeDirGetter;
    }

    /**
     * Get the defined node version.
     *
     * @return node version
     */
    public String getNodeVersion() {
        return nodeVersion;
    }

    /**
     * Get the node download root to be used for downloading node.
     *
     * @return node download root
     */
    public URI getNodeDownloadRoot() {
        return nodeDownloadRoot;
    }

    /**
     * Check if version checks should be ignored.
     *
     * @return ignore version checks
     */
    public boolean isIgnoreVersionChecks() {
        return ignoreVersionChecks;
    }

    /**
     * Check if alternative node usage should be forced.
     *
     * @return force alternative node usage
     */
    public boolean isForceAlternativeNode() {
        return forceAlternativeNode;
    }

    /**
     * Check if global pnpm should be used.
     *
     * @return use global pnpm
     */
    public boolean isUseGlobalPnpm() {
        return useGlobalPnpm;
    }

    /**
     * Check if automatic updates are enabled.
     *
     * @return automatic update
     */
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
