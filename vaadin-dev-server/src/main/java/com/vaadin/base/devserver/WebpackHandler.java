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

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.base.devserver.DevServerOutputFinder.Result;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a webpack dev server and provides files handles getting resources from
 * <code>webpack-dev-server</code>.
 * <p>
 * This class is meant to be used during developing time. For a production mode
 * site <code>webpack</code> generates the static bundles that will be served
 * directly from the servlet (using a default servlet if such exists) or through
 * a stand alone static file server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public final class WebpackHandler extends AbstractDevServerRunner {

    // It's not possible to know whether webpack is ready unless reading output
    // messages. When webpack finishes, it writes either a `Compiled` or a
    // `Failed` in the last line
    private static final String DEFAULT_OUTPUT_PATTERN = ": Compiled.";
    private static final String DEFAULT_ERROR_PATTERN = ": Failed to compile.";

    /**
     * The local installation path of the webpack-dev-server node script.
     */
    public static final String WEBPACK_SERVER = "node_modules/webpack-dev-server/bin/webpack-dev-server.js";

    /**
     * The list of static resource paths from webpack manifest.
     */
    private volatile List<String> manifestPaths = new ArrayList<>();

    private WebpackHandler(Lookup lookup, int runningPort, File npmFolder,
            CompletableFuture<Void> waitFor) {
        super(lookup, runningPort, npmFolder, waitFor);
    }

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param lookup
     *            the provided lookup to get required data
     * @param npmFolder
     *            folder with npm configuration files
     * @param waitFor
     *            a completable future whose execution result needs to be
     *            available to start the webpack dev server
     *
     * @return the instance in case everything is alright, null otherwise
     */
    public static WebpackHandler start(Lookup lookup, File npmFolder,
            CompletableFuture<Void> waitFor) {
        return start(0, lookup, npmFolder, waitFor);
    }

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param runningPort
     *            port on which Webpack is listening.
     * @param lookup
     *            the provided lookup to get required data
     * @param npmFolder
     *            folder with npm configuration files
     * @param waitFor
     *            a completable future whose execution result needs to be
     *            available to start the webpack dev server
     *
     * @return the instance in case everything is alright, null otherwise
     */
    public static WebpackHandler start(int runningPort, Lookup lookup,
            File npmFolder, CompletableFuture<Void> waitFor) {
        ApplicationConfiguration configuration = lookup
                .lookup(ApplicationConfiguration.class);
        if (configuration.isProductionMode()
                || !configuration.enableDevServer()) {
            return null;
        }
        return new WebpackHandler(lookup, runningPort, npmFolder, waitFor);
    }

    @Override
    protected boolean checkConnection() {
        try {
            return readManifestPaths();
        } catch (IOException e) {
            getLogger().debug("Error checking webpack dev server connection",
                    e);
        }
        return false;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(WebpackHandler.class);
    }

    @Override
    public boolean isDevModeRequest(HttpServletRequest request) {
        boolean devmodeRequest = super.isDevModeRequest(request);
        if (devmodeRequest) {
            return true;
        } else {
            String pathInfo = request.getPathInfo();
            return manifestPaths.contains(pathInfo);
        }
    }

    /**
     * Get and parse /manifest.json from webpack-dev-server, extracting paths to
     * all resources in the webpack output.
     *
     * Those paths do not necessarily start with /VAADIN, as some resources must
     * be served from the root directory, e. g., service worker JS.
     *
     * @return true if reading succeeded, false otherwise
     * @throws IOException
     */
    private boolean readManifestPaths() throws IOException {
        getLogger().debug("Reading manifest.json from webpack");
        HttpURLConnection connection = prepareConnection("/manifest.json",
                "GET");
        int responseCode = connection.getResponseCode();
        if (responseCode != HTTP_OK) {
            getLogger().error(
                    "Unable to get manifest.json from "
                            + "webpack-dev-server, got {} {}",
                    responseCode, connection.getResponseMessage());
            return false;
        }

        String manifestJson = FrontendUtils
                .streamToString(connection.getInputStream());
        manifestPaths = FrontendUtils.parseManifestPaths(manifestJson);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(
                    "Got asset paths from webpack manifest.json: \n    {}",
                    String.join("\n    ", manifestPaths));
        }
        return true;
    }

    @Override
    protected void onDevServerCompilation(Result result) {
        super.onDevServerCompilation(result);
        try {
            readManifestPaths();
        } catch (IOException e) {
            getLogger().error(
                    "Error when reading manifest.json from " + getServerName(),
                    e);
        }

        // trigger a live-reload since webpack has recompiled the bundle
        // if failure, ensures the webpack error is shown in the browser
        triggerLiveReload();
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
        command.add("--watch-options-stdin"); // Tell wds to stop even if
                                              // watchDog fail

        String customParameters = getApplicationConfiguration()
                .getStringProperty(
                        InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
                        "");
        if (!customParameters.isEmpty()) {
            command.addAll(Arrays.asList(customParameters.split(" +")));
            getLogger().info("Starting {} using: {}", getServerName(),
                    String.join(" ", command));
        } else {
            command.add("--devtool=eval-source-map");
            command.add("--mode=development");
        }

        return command;
    }

    @Override
    protected String getServerName() {
        return "Webpack";
    }

    @Override
    protected File getServerBinary() {
        return new File(getNpmFolder(), WEBPACK_SERVER);
    }

    @Override
    protected File getServerConfig() {
        return new File(getNpmFolder(), FrontendUtils.WEBPACK_CONFIG);
    }

    @Override
    protected Pattern getServerSuccessPattern() {
        return Pattern.compile(getApplicationConfiguration().getStringProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN,
                DEFAULT_OUTPUT_PATTERN));
    }

    @Override
    protected Pattern getServerFailurePattern() {
        return Pattern.compile(getApplicationConfiguration().getStringProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN,
                DEFAULT_ERROR_PATTERN));
    }

}
