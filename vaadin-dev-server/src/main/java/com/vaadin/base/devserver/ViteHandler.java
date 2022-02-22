/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * The local installation path of the server node script.
     */
    public static final String VITE_SERVER = "node_modules/vite/bin/vite.js";

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
        command.add(getContextPath() + "/" + VAADIN_MAPPING);

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
        return new File(getProjectRoot(), VITE_SERVER);
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
        return Pattern.compile("ready in .*ms");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ViteHandler.class);
    }

    @Override
    public HttpURLConnection prepareConnection(String path, String method)
            throws IOException {
        if ("/index.html".equals(path)) {
            return super.prepareConnection(
                    getContextPath() + "/" + VAADIN_MAPPING + "index.html",
                    method);
        }

        if ("/sw.js".equals(path)) {
            return super.prepareConnection(
                getContextPath() + "/" + VAADIN_MAPPING + "sw.js",
                method);
        }

        return super.prepareConnection(getContextPath() + path, method);
    }

    private String getContextPath() {
        VaadinServletContext servletContext = (VaadinServletContext) getApplicationConfiguration()
                .getContext();
        return servletContext.getContext().getContextPath();
    }
}
