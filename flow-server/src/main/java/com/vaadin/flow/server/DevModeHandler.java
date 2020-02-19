/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.server.frontend.FrontendUtils.GREEN;
import static com.vaadin.flow.server.frontend.FrontendUtils.RED;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.YELLOW;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.FrontendUtils.console;
import static com.vaadin.flow.server.frontend.FrontendUtils.getNodeExecutable;
import static com.vaadin.flow.server.frontend.FrontendUtils.validateNodeAndNpmVersion;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Handles getting resources from <code>webpack-dev-server</code>.
 * <p>
 * This class is meant to be used during developing time. For a production mode
 * site <code>webpack</code> generates the static bundles that will be served
 * directly from the servlet (using a default servlet if such exists) or through
 * a stand alone static file server.
 *
 * By default it keeps updated npm dependencies and node imports before running
 * webpack server
 *
 * @since 2.0
 */
public final class DevModeHandler implements RequestHandler {

    private static final AtomicReference<DevModeHandler> atomicHandler = new AtomicReference<>();

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
    private static final String LOG_END = "Started webpack-dev-server. Time: {}ms";

    // If after this time in millisecs, the pattern was not found, we unlock the
    // process and continue. It might happen if webpack changes their output
    // without advise.
    private static final String DEFAULT_TIMEOUT_FOR_PATTERN = "60000";

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private static final int DEFAULT_TIMEOUT = 120 * 1000;
    private static final String WEBPACK_HOST = "http://localhost";

    private boolean notified = false;

    private volatile String failedOutput;

    /**
     * The local installation path of the webpack-dev-server node script.
     */
    public static final String WEBPACK_SERVER = "node_modules/webpack-dev-server/bin/webpack-dev-server.js";

    private volatile int port;
    private final AtomicReference<Process> webpackProcess = new AtomicReference<>();
    private final boolean reuseDevServer;
    private final AtomicReference<DevServerWatchDog> watchDog = new AtomicReference<>();

    private StringBuilder cumulativeOutput = new StringBuilder();

    private final AtomicReference<Throwable> executionException = new AtomicReference<>();
    private final AtomicBoolean devServerStarted = new AtomicBoolean();

    private DevModeHandler(DeploymentConfiguration config, int runningPort,
            File npmFolder, File webpack, File webpackConfig,
            CompletableFuture<Void> waitFor) {

        port = runningPort;
        reuseDevServer = config.reuseDevServer();

        waitFor.whenCompleteAsync((value, exception) -> {
            try {
                runOnFutureComplete(value, exception, config, npmFolder,
                        webpack, webpackConfig);
            } finally {
                devServerStarted.set(true);
            }
            throw new IllegalStateException(format(
                    "webpack-dev-server port '%d' is defined but it's not working properly",
                    port));
        }

        long start = System.nanoTime();
        getLogger().info("Starting webpack-dev-server");

        watchDog = new DevServerWatchDog();

        // Look for a free port
        port = getFreePort();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(npmFolder);

        validateNodeAndNpmVersion(npmFolder.getAbsolutePath());

        boolean useHomeNodeExec = config.getBooleanProperty(
                Constants.REQUIRE_HOME_NODE_EXECUTABLE, false);

        String nodeExec = null;
        if (useHomeNodeExec) {
            nodeExec = FrontendUtils.ensureNodeExecutableInHome();
        } else {
            nodeExec = getNodeExecutable(npmFolder.getAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(nodeExec);
        command.add(webpack.getAbsolutePath());
        command.add("--config");
        command.add(webpackConfig.getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(port));
        command.add("--watchDogPort=" + watchDog.getWatchDogPort());
        command.addAll(Arrays.asList(config
                .getStringProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
                        "-d --inline=false --progress --colors")
                .split(" +")));

        console(GREEN, START);
        console(YELLOW, commandToString(npmFolder.getAbsolutePath(), command));

        processBuilder.command(command);
        try {
            webpackProcess = processBuilder
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

            logStream(webpackProcess.getInputStream(), succeed, failure);

            getLogger().info(LOG_START);
            synchronized (this) {
                this.wait(Integer.parseInt(config.getStringProperty( // NOSONAR
                        SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT,
                        DEFAULT_TIMEOUT_FOR_PATTERN)));
            }

            if (!webpackProcess.isAlive()) {
                throw new IllegalStateException("Webpack exited prematurely");
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().info(LOG_END, ms);

        } catch (IOException | InterruptedException e) {
            getLogger().error("Failed to start the webpack process", e);
        }

        saveRunningDevServerPort();
=======
        });>>>>>>>2721ec 3

    Run (p)npm install and webpack dev server in a separate thread without blocking servlet container initializer
    }

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param configuration
     *            deployment configuration
     * @param npmFolder
     *            folder with npm configuration files
     *
     * @return the instance in case everything is alright, null otherwise
     */
    public static DevModeHandler start(DeploymentConfiguration configuration,
            File npmFolder, CompletableFuture<Void> waitFor) {
        return start(0, configuration, npmFolder, waitFor);
    }

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param runningPort
     *            port on which Webpack is listening.
     * @param configuration
     *            deployment configuration
     * @param npmFolder
     *            folder with npm configuration files
     *
     * @return the instance in case everything is alright, null otherwise
     */
    public static DevModeHandler start(int runningPort,
            DeploymentConfiguration configuration, File npmFolder,
            CompletableFuture<Void> waitFor) {
        if (configuration.isProductionMode()
                || !configuration.enableDevServer()) {
            return null;
        }
        atomicHandler.compareAndSet(null,
                createInstance(runningPort, configuration, npmFolder, waitFor));
        return getDevModeHandler();
    }

    /**
     * Get the instantiated DevModeHandler.
     *
     * @return devModeHandler or {@code null} if not started
     */
    public static DevModeHandler getDevModeHandler() {
        return atomicHandler.get();
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        Throwable throwable = executionException.get();
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable != null) {
            throw new IllegalStateException(throwable);
        }
        if (devServerStarted.get()) {
            return false;
        } else {
            IOUtils.write("<html>"
                    + "  <head><meta http-equiv='refresh' content='3'></head>"
                    + "  <body> The webpack dev server has not yet started. Please wait... </body>"
                    + "</html>", response.getOutputStream(),
                    StandardCharsets.UTF_8);
            return true;
        }
    }

    private static DevModeHandler createInstance(int runningPort,
            DeploymentConfiguration configuration, File npmFolder,
            CompletableFuture<Void> waitFor) {

        File webpack = null;
        File webpackConfig = null;
        if (runningPort == 0) {
            runningPort = getRunningDevServerPort();
        }

        // Skip checks if we have a webpack-dev-server already running
        if (runningPort == 0) {
            webpack = new File(npmFolder, WEBPACK_SERVER);
            webpackConfig = new File(npmFolder, WEBPACK_CONFIG);
            if (!npmFolder.exists()) {
                getLogger().warn(
                        "Instance not created because cannot change to '{}'",
                        npmFolder);
                return null;
            }
            if (!webpack.canExecute()) {
                getLogger().warn(
                        "Instance not created because cannot execute '{}'. Did you run `npm install`",
                        webpack);
                return null;
            } else if (!webpack.exists()) {
                getLogger().warn(
                        "Instance not created because file '{}' doesn't exist. Did you run `npm install`",
                        webpack);
                return null;
            }
            if (!webpackConfig.canRead()) {
                getLogger().warn(
                        "Instance not created because there is not webpack configuration '{}'",
                        webpackConfig);
                return null;
            }
        }
        return new DevModeHandler(configuration, runningPort, npmFolder,
                webpack, webpackConfig, waitFor);
    }

    /**
     * Returns true if it's a request that should be handled by webpack.
     *
     * @param request
     *            the servlet request
     * @return true if the request should be forwarded to webpack
     */
    public boolean isDevModeRequest(HttpServletRequest request) {
        return request.getPathInfo() != null
                && request.getPathInfo().startsWith("/" + VAADIN_MAPPING);
    }

    /**
     * Serve a file by proxying to webpack.
     * <p>
     * Note: it considers the {@link HttpServletRequest#getPathInfo} that will
     * be the path passed to the 'webpack-dev-server' which is running in the
     * context root folder of the application.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return false if webpack returned a not found, true otherwise
     * @throws IOException
     *             in the case something went wrong like connection refused
     */
    public boolean serveDevModeRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // TODO : show 'error' message if dev server is not yet started

        // Since we have 'publicPath=/VAADIN/' in webpack config,
        // a valid request for webpack-dev-server should start with '/VAADIN/'
        String requestFilename = request.getPathInfo();

        HttpURLConnection connection = prepareConnection(requestFilename,
                request.getMethod());

        // Copies all the headers from the original request
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            connection.setRequestProperty(header,
                    // Exclude keep-alive
                    "Connect".equals(header) ? "close"
                            : request.getHeader(header));
        }

        // Send the request
        getLogger().debug("Requesting resource to webpack {}",
                connection.getURL());
        int responseCode = connection.getResponseCode();
        if (responseCode == HTTP_NOT_FOUND) {
            getLogger().debug("Resource not served by webpack {}",
                    requestFilename);
            // webpack cannot access the resource, return false so as flow can
            // handle it
            return false;
        }
        getLogger().debug("Served resource by webpack: {} {}", responseCode,
                requestFilename);

        // Copies response headers
        connection.getHeaderFields().forEach((header, values) -> {
            if (header != null) {
                response.addHeader(header, values.get(0));
            }
        });

        if (responseCode == HTTP_OK) {
            // Copies response payload
            writeStream(response.getOutputStream(),
                    connection.getInputStream());
        } else if (responseCode < 400) {
            response.setStatus(responseCode);
        } else {
            // Copies response code
            response.sendError(responseCode);
        }

        // Close request to avoid issues in CI and Chrome
        response.getOutputStream().close();

        return true;
    }

    private boolean checkWebpackConnection() {
        try {
            prepareConnection("/", "GET").getResponseCode();
            return true;
        } catch (IOException e) {
            getLogger().debug("Error checking webpack dev server connection",
                    e);
        }
        return false;
    }

    /**
     * Prepare a HTTP connection against webpack-dev-server.
     *
     * @param path
     *            the file to request
     * @param method
     *            the http method to use
     * @return the connection
     * @throws IOException
     *             on connection error
     */
    public HttpURLConnection prepareConnection(String path, String method)
            throws IOException {
        URL uri = new URL(WEBPACK_HOST + ":" + getPort() + path);
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod(method);
        connection.setReadTimeout(DEFAULT_TIMEOUT);
        connection.setConnectTimeout(DEFAULT_TIMEOUT);
        return connection;
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

        // save output so as it can be used to alert user in browser.
        cumulativeOutput.append(cleanLine);

        boolean succeed = success.matcher(line).find();
        boolean failed = failure.matcher(line).find();
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

            // Notify DevModeHandler to continue
            doNotify();
        }
    }

    private void writeStream(ServletOutputStream outputStream,
            InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        while ((bytes = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytes);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeHandler.class);
    }

    /**
     * Return webpack console output when a compilation error happened.
     *
     * @return console output if error or null otherwise.
     */
    public String getFailedOutput() {
        return failedOutput;
    }

    /**
     * Remove the running port from the vaadinContext and temporary file.
     */
    public void removeRunningDevServerPort() {
        FileUtils.deleteQuietly(LazyDevServerPortFileInit.DEV_SERVER_PORT_FILE);
    }

    private void runOnFutureComplete(Void value, Throwable exception,
            DeploymentConfiguration config, File npmFolder, File webpack,
            File webpackConfig) {
        if (exception == null) {
            try {
                doStartDevModeServer(config, npmFolder, webpack, webpackConfig);
            } catch (Throwable throwable) {
                executionException.set(throwable);
                getLogger().error(null, throwable);
            }
        } else if (exception instanceof CompletionException) {
            Throwable cause = ((CompletionException) exception).getCause();
            executionException.set(cause);
            getLogger().error(null, cause);
        } else {
            executionException.set(exception);
            getLogger().error(null, exception);
        }
    }

    private void saveRunningDevServerPort() {
        File portFile = LazyDevServerPortFileInit.DEV_SERVER_PORT_FILE;
        try {
            FileUtils.writeStringToFile(portFile, String.valueOf(port),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void doStartDevModeServer(DeploymentConfiguration config,
            File npmFolder, File webpack, File webpackConfig)
            throws ExecutionFailedException {
        // If port is defined, means that webpack is already running
        if (port > 0) {
            if (checkWebpackConnection()) {
                getLogger().info("Reusing webpack-dev-server running at {}:{}",
                        WEBPACK_HOST, port);

                // Save running port for next usage
                saveRunningDevServerPort();
                watchDog.set(null);
                return;
            }
            throw new ExecutionFailedException(format(
                    "webpack-dev-server port '%d' is defined but it's not working properly",
                    port));
        }

        long start = System.nanoTime();
        getLogger().info("Starting webpack-dev-server");

        DevServerWatchDog watchDogInstance = new DevServerWatchDog();
        watchDog.set(watchDogInstance);

        // Look for a free port
        port = getFreePort();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(npmFolder);

        validateNodeAndNpmVersion(npmFolder.getAbsolutePath());

        List<String> command = new ArrayList<>();
        command.add(getNodeExecutable(npmFolder.getAbsolutePath()));
        command.add(webpack.getAbsolutePath());
        command.add("--config");
        command.add(webpackConfig.getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(port));
        command.add("--watchDogPort=" + watchDogInstance.getWatchDogPort());
        command.addAll(Arrays.asList(config
                .getStringProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
                        "-d --inline=false --progress --colors")
                .split(" +")));

        console(GREEN, START);
        console(YELLOW, commandToString(npmFolder.getAbsolutePath(), command));

        processBuilder.command(command);
        try {
            Process process = processBuilder
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectErrorStream(true).start();

            webpackProcess.set(process);

            // We only can save the webpackProcess reference the first time
            // that the DevModeHandler is created. There is no way to store
            // it in the servlet container, and we do not want to save it in
            // the global JVM.
            // We instruct the JVM to stop the webpack-dev-server daemon
            // when the JVM stops, to avoid leaving daemons running in the
            // system.
            // NOTE: that in the corner case that the JVM crashes or it is
            // killed the daemon will be kept running. But anyways it will also
            // happen if the system was configured to be stop the daemon when
            // the servlet context is destroyed.
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

            if (!process.isAlive()) {
                throw new ExecutionFailedException(
                        "Webpack exited prematurely");
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().info(LOG_END, ms);

        } catch (IOException | InterruptedException e) {
            getLogger().error("Failed to start the webpack process", e);
        }

        saveRunningDevServerPort();
    }

    private static int getRunningDevServerPort() {
        int port = 0;
        File portFile = LazyDevServerPortFileInit.DEV_SERVER_PORT_FILE;
        if (portFile.canRead()) {
            try {
                String portString = FileUtils
                        .readFileToString(portFile, StandardCharsets.UTF_8)
                        .trim();
                if (!portString.isEmpty()) {
                    port = Integer.parseInt(portString);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return port;
    }

    /**
     * Returns an available tcp port in the system.
     *
     * @return a port number which is not busy
     */
    static int getFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to find a free port for running webpack", e);
        }
    }

    /**
     * Get the listening port of the 'webpack-dev-server'.
     *
     * @return the listening port of webpack
     */
    public int getPort() {
        return port;
    }

    /**
     * Whether the 'webpack-dev-server' should be reused on servlet reload.
     * Default true.
     *
     * @return true in case of reusing the server.
     */
    public boolean reuseDevServer() {
        return reuseDevServer;
    }

    /**
     * Stop the webpack-dev-server.
     */
    public void stop() {
        if (atomicHandler.get() == null) {
            return;
        }

        try {
            // The most reliable way to stop the webpack-dev-server is
            // by informing webpack to exit. We have implemented in webpack a
            // a listener that handles the stop command via HTTP and exits.
            prepareConnection("/stop", "GET").getResponseCode();
        } catch (IOException e) {
            getLogger().debug(
                    "webpack-dev-server does not support the `/stop` command.",
                    e);
        }

        DevServerWatchDog watchDogInstance = watchDog.get();
        if (watchDogInstance != null) {
            watchDogInstance.stop();
        }

        Process process = webpackProcess.get();
        if (process != null && process.isAlive()) {
            process.destroy();
        }

        atomicHandler.set(null);
        removeRunningDevServerPort();
    }

    <<<<<<<Upstream,

    based on master

    private static final class LazyDevServerPortFileInit {

        private static final File DEV_SERVER_PORT_FILE = createDevServerPortFile();

        private static File createDevServerPortFile() {
            try {
                return File.createTempFile("flow-dev-server", "port");
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }=======>>>>>>>2721ec 3

    Run (p)npm install and webpack dev server in a separate thread without blocking servlet container initializer
}
