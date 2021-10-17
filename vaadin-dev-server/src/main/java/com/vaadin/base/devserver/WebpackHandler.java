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

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.frontend.FrontendUtils.GREEN;
import static com.vaadin.flow.server.frontend.FrontendUtils.RED;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.FrontendUtils.console;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
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
    private static final String FAILED_MSG = "\n------------------ Frontend compilation failed. ------------------\n\n";
    private static final String SUCCEED_MSG = "\n----------------- Frontend compiled successfully. -----------------\n\n";
    private static final String START = "\n------------------ Starting Frontend compilation. ------------------\n";
    private static final String END = "\n------------------------- Webpack stopped  -------------------------\n";
    private static final String LOG_START = "Running webpack to compile frontend resources. This may take a moment, please stand by...";

    // If after this time in millisecs, the pattern was not found, we unlock the
    // process and continue. It might happen if webpack changes their output
    // without advise.
    private static final String DEFAULT_TIMEOUT_FOR_PATTERN = "60000";

    private boolean notified = false;

    private volatile String failedOutput;

    /**
     * The local installation path of the webpack-dev-server node script.
     */
    public static final String WEBPACK_SERVER = "node_modules/webpack-dev-server/bin/webpack-dev-server.js";

    /**
     * The list of static resource paths from webpack manifest.
     */
    private volatile List<String> manifestPaths = new ArrayList<>();

    private StringBuilder cumulativeOutput = new StringBuilder();

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

    private synchronized void doNotify() {
        if (!notified) {
            notified = true;
            notifyAll(); // NOSONAR
        }
    }

    // mirrors a stream to logger, and check whether a success or error pattern
    // is found in the output.
    private void logStream(InputStream input, Pattern success,
            Pattern failure) {
        Thread thread = new Thread(() -> {
            InputStreamReader reader = new InputStreamReader(input,
                    StandardCharsets.UTF_8);
            try {
                readLinesLoop(success, failure, reader);
            } catch (IOException e) {
                if ("Stream closed".equals(e.getMessage())) {
                    console(GREEN, END);
                    getLogger().debug("Exception when reading webpack output.",
                            e);
                } else {
                    getLogger().error("Exception when reading webpack output.",
                            e);
                }
            }

            // Process closed stream, means that it exited, notify
            // DevModeHandler to continue
            doNotify();
        });
        thread.setDaemon(true);
        thread.setName("webpack");
        thread.start();
    }

    private void readLinesLoop(Pattern success, Pattern failure,
            InputStreamReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i; (i = reader.read()) >= 0;) {
            char ch = (char) i;
            console("%c", ch);
            line.append(ch);
            if (ch == '\n') {
                processLine(line.toString(), success, failure);
                line.setLength(0);
            }
        }
    }

    private void processLine(String line, Pattern success, Pattern failure) {
        // skip progress lines
        if (line.contains("\b")) {
            return;
        }

        // remove color escape codes for console
        String cleanLine = line.replaceAll("(\u001b\\[[;\\d]*m|[\b\r]+)", "");

        boolean succeed = success.matcher(line).find();
        boolean failed = failure.matcher(line).find();

        // save output so as it can be used to alert user in browser, unless
        // it's `: Failed to compile.`
        if (!failed) {
            cumulativeOutput.append(cleanLine);
        }

        // We found the success or failure pattern in stream
        if (succeed || failed) {
            if (succeed) {
                console(GREEN, SUCCEED_MSG);
            } else {
                console(RED, FAILED_MSG);
            }
            // save output in case of failure
            failedOutput = failed ? cumulativeOutput.toString() : null;

            // reset cumulative buffer for the next compilation
            cumulativeOutput = new StringBuilder();

            // Read webpack asset manifest json
            try {
                readManifestPaths();
            } catch (IOException e) {
                getLogger().error("Error when reading manifest.json "
                        + "from webpack-dev-server", e);
            }

            // Notify DevModeHandler to continue
            doNotify();

            // trigger a live-reload since webpack has recompiled the bundle
            // if failure, ensures the webpack error is shown in the browser
            triggerLiveReload();
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(WebpackHandler.class);
    }

    @Override
    public String getFailedOutput() {
        return failedOutput;
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
    protected Process doStartWebpack() {
        ApplicationConfiguration config = getApplicationConfiguration();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(getNpmFolder());

        boolean useHomeNodeExec = config.getBooleanProperty(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, false);
        boolean nodeAutoUpdate = config
                .getBooleanProperty(InitParameters.NODE_AUTO_UPDATE, false);
        boolean useGlobalPnpm = config.getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM, false);

        FrontendToolsSettings settings = new FrontendToolsSettings(
                getNpmFolder().getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setForceAlternativeNode(useHomeNodeExec);
        settings.setAutoUpdate(nodeAutoUpdate);
        settings.setUseGlobalPnpm(useGlobalPnpm);

        FrontendTools tools = new FrontendTools(settings);
        tools.validateNodeAndNpmVersion();

        String nodeExec = null;
        if (useHomeNodeExec) {
            nodeExec = tools.forceAlternativeNodeExecutable();
        } else {
            nodeExec = tools.getNodeExecutable();
        }

        List<String> command = makeCommands(config, getServerBinary(),
                getServerConfig(), nodeExec);

        console(GREEN, START);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(
                    commandToString(getNpmFolder().getAbsolutePath(), command));
        }

        processBuilder.command(command);

        try {
            Process process = processBuilder
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectErrorStream(true).start();
            // We only can save the webpackProcess reference the first time that
            // the DevModeHandler is created. There is no way to store
            // it in the servlet container, and we do not want to save it in the
            // global JVM.
            // We instruct the JVM to stop the webpack-dev-server daemon when
            // the JVM stops, to avoid leaving daemons running in the system.
            // NOTE: that in the corner case that the JVM crashes or it is
            // killed
            // the daemon will be kept running. But anyways it will also happens
            // if the system was configured to be stop the daemon when the
            // servlet context is destroyed.
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            Pattern succeed = Pattern.compile(config.getStringProperty(
                    SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN,
                    DEFAULT_OUTPUT_PATTERN));

            Pattern failure = Pattern.compile(config.getStringProperty(
                    SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN,
                    DEFAULT_ERROR_PATTERN));

            logStream(process.getInputStream(), succeed, failure);

            getLogger().info(LOG_START);
            synchronized (this) {
                this.wait(Integer.parseInt(config.getStringProperty( // NOSONAR
                        SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT,
                        DEFAULT_TIMEOUT_FOR_PATTERN)));
            }

            return process;
        } catch (IOException e) {
            getLogger().error("Failed to start the webpack process", e);
        } catch (InterruptedException e) {
            getLogger().debug("Webpack process start has been interrupted", e);
        }
        return null;
    }

    private List<String> makeCommands(ApplicationConfiguration config,
            File webpack, File webpackConfig, String nodeExec) {
        List<String> command = new ArrayList<>();
        command.add(nodeExec);
        command.add(webpack.getAbsolutePath());
        command.add("--config");
        command.add(webpackConfig.getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(getPort()));
        command.add("--watch-options-stdin"); // Tell wds to stop even if
                                              // watchDog fail
        command.add("--env");
        command.add("watchDogPort=" + getWatchDog().getWatchDogPort());
        command.addAll(Arrays.asList(config
                .getStringProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
                        "--devtool=eval-source-map --mode=development")
                .split(" +")));
        return command;
    }

    @Override
    protected File getServerBinary() {
        return new File(getNpmFolder(), WEBPACK_SERVER);
    }

    @Override
    protected File getServerConfig() {
        return new File(getNpmFolder(), FrontendUtils.WEBPACK_CONFIG);
    }

}
