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
package com.vaadin.base.devserver;

import static java.net.HttpURLConnection.HTTP_OK;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.frontend.FrontendUtils;

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
    protected boolean checkConnection() {
        try {
            getLogger().debug("Checking vite connection");
            HttpURLConnection connection = prepareConnection("/@vite/client",
                    "GET");
            int responseCode = connection.getResponseCode();
            return (responseCode == HTTP_OK);
        } catch (IOException e) {
            getLogger().debug("Error checking vite connection", e);
        }
        return false;
    }

    @Override
    protected List<String> getServerStartupCommand(String nodeExec) {
        List<String> command = new ArrayList<>();
        command.add(nodeExec);
        command.add(getServerBinary().getAbsolutePath());
        command.add("--config");
        command.add(getServerConfig().getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(getPort()));
        return command;
    }

    @Override
    protected String getServerName() {
        return "Vite";
    }

    @Override
    protected File getServerBinary() {
        return new File(getNpmFolder(), VITE_SERVER);
    }

    @Override
    protected File getServerConfig() {
        return new File(getNpmFolder(), FrontendUtils.VITE_CONFIG);
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

}
