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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FrontendTools;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;

/**
 * Handles communication with a Vite server.
 * <p>
 * This class is meant to be used during developing time.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public final class ViteHandler extends AbstractDevServerRunner {
    /**
     * Files that are loaded from the root path but Vite places them in the
     * VAADIN folder.
     */
    private static final String[] FILES_IN_ROOT = new String[] {
            FrontendUtils.INDEX_HTML, FrontendUtils.WEB_COMPONENT_HTML,
            FrontendUtils.SERVICE_WORKER_SRC_JS };
    private static final Pattern SERVER_RESTARTED_PATTERN = Pattern
            .compile("\\[vite] server restart(ed| failed)");
    private static final Pattern SERVER_RESTARTING_PATTERN = Pattern
            .compile("\\[vite].*restarting server\\.\\.\\.");
    private static final Pattern SERVER_SUCCESS_PATTERN = Pattern
            .compile("ready in .*ms");

    /**
     * Creates and starts the dev mode handler if none has been started yet.
     *
     * @param lookup
     *            the provided lookup to get required data
     * @param runningPort
     *            a port on which Vite is already running or 0 to start a new
     *            process
     * @param npmFolder
     *            folder with npm configuration files
     * @param waitFor
     *            a completable future whose execution result needs to be
     *            available to start the dev server
     */
    public ViteHandler(Lookup lookup, int runningPort, File npmFolder,
            CompletableFuture<Void> waitFor) {
        super(lookup, runningPort, npmFolder, waitFor);
    }

    @Override
    protected List<String> getServerStartupCommand(
            FrontendTools frontendTools) {
        List<String> command = new ArrayList<>();
        command.add(frontendTools.getNodeExecutable());
        command.add(getServerBinary().getAbsolutePath());
        command.add("--config");
        command.add(getServerConfig().getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(getPort()));
        command.add("--base");
        command.add(getPathToVaadin());

        String customParameters = getApplicationConfiguration()
                .getStringProperty(
                        InitParameters.SERVLET_PARAMETER_DEVMODE_VITE_OPTIONS,
                        "");
        if (!customParameters.isEmpty()) {
            command.addAll(Arrays.asList(customParameters.split(" +")));
            getLogger().info("Starting {} using: {}", getServerName(),
                    String.join(" ", command)); // NOSONAR
        }

        return command;
    }

    @Override
    protected String getServerName() {
        return "Vite";
    }

    @Override
    protected File getServerBinary() {
        try {
            return getFrontendTools()
                    .getNpmPackageExecutable("vite", "vite", getProjectRoot())
                    .toFile();
        } catch (FrontendUtils.CommandExecutionException e) {
            throw new RuntimeException("Vite not found", e);
        }
    }

    @Override
    protected File getServerConfig() {
        return new File(getProjectRoot(), FrontendUtils.VITE_CONFIG);
    }

    @Override
    protected Pattern getServerFailurePattern() {
        // Vite fails if the config is invalid, then the process exists
        // Otherwise, errors are reported later on.
        return null;
    }

    @Override
    protected Pattern getServerSuccessPattern() {
        return SERVER_SUCCESS_PATTERN;
    }

    @Override
    protected Pattern getServerRestartingPattern() {
        return SERVER_RESTARTING_PATTERN;
    }

    @Override
    protected Pattern getServerRestartedPattern() {
        return SERVER_RESTARTED_PATTERN;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ViteHandler.class);
    }

    @Override
    public HttpURLConnection prepareConnection(String path, String method)
            throws IOException {
        for (String fileInSerlvetPath : FILES_IN_ROOT) {
            if (path.equals("/" + fileInSerlvetPath)) {
                return super.prepareConnection(
                        getPathToVaadin() + fileInSerlvetPath, method);
            }
        }

        // The path passed to this method starts with /VAADIN and
        // getPathToVaadin() also
        // includes /VAADIN so one needs to be removed
        String vitePath = getPathToVaadin().replace("/" + VAADIN_MAPPING, "")
                + path;
        return super.prepareConnection(vitePath, method);
    }

    /**
     * Gets the url path to the /VAADIN folder.
     *
     * @return the url path to the /VAADIN folder, relative to the host root
     */
    public String getPathToVaadin() {
        return getContextPath() + getPathToVaadinInContext();
    }

    /**
     * Gets the url path to the /VAADIN folder inside the context root.
     *
     * @return the url path to the /VAADIN folder, relative to the context root
     */
    public String getPathToVaadinInContext() {
        return FrontendUtils.getFrontendServletPath(
                getServletContext().getContext()) + "/" + VAADIN_MAPPING;
    }

    private String getContextPath() {
        VaadinServletContext servletContext = getServletContext();
        return servletContext.getContext().getContextPath();
    }

    private VaadinServletContext getServletContext() {
        return (VaadinServletContext) getApplicationConfiguration()
                .getContext();
    }

}
