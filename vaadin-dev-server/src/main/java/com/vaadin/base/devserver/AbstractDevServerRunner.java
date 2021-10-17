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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with most details of starting a frontend development server or
 * connecting to an existing one.
 * <p>
 * This class is meant to be used during developing time.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class AbstractDevServerRunner implements DevModeHandler {

    protected static final String START_FAILURE = "Couldn't start dev server because";

    private static final String WEBPACK_HOST = "http://localhost";

    /**
     * UUID system property for identifying JVM restart.
     */
    private static final String WEBPACK_PORTFILE_UUID_PROPERTY = "vaadin.frontend.webpack.portfile.uuid";

    // webpack dev-server allows " character if passed through, need to
    // explicitly check requests for it
    private static final Pattern WEBPACK_ILLEGAL_CHAR_PATTERN = Pattern
            .compile("\"|%22");

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private static final int DEFAULT_TIMEOUT = 120 * 1000;

    private final File npmFolder;
    private volatile int port;
    private final AtomicReference<Process> webpackProcess = new AtomicReference<>();
    private final boolean reuseDevServer;
    private final File devServerPortFile;

    private AtomicBoolean isDevServerFailedToStart = new AtomicBoolean();

    private transient BrowserLiveReload liveReload;

    private final CompletableFuture<Void> devServerStartFuture;

    private final AtomicReference<DevServerWatchDog> watchDog = new AtomicReference<>();

    private boolean usingAlreadyStartedProcess = false;

    private ApplicationConfiguration applicationConfiguration;

    /**
     * Craete an instance that waits for the given task to complete before
     * starting or connecting to the server.
     * 
     * @param lookup
     *            a lookup instance
     * @param runningPort
     *            the port that a dev server is already running on or 0 to start
     *            a new server
     * @param npmFolder
     *            the project root
     * @param waitFor
     *            the task to wait for before running the server.
     */
    protected AbstractDevServerRunner(Lookup lookup, int runningPort,
            File npmFolder, CompletableFuture<Void> waitFor) {
        this.npmFolder = npmFolder;
        port = runningPort;
        applicationConfiguration = lookup
                .lookup(ApplicationConfiguration.class);
        reuseDevServer = applicationConfiguration.reuseDevServer();
        devServerPortFile = getDevServerPortFile(npmFolder);

        BrowserLiveReloadAccessor liveReloadAccess = lookup
                .lookup(BrowserLiveReloadAccessor.class);
        liveReload = liveReloadAccess != null
                ? liveReloadAccess
                        .getLiveReload(applicationConfiguration.getContext())
                : null;

        BiConsumer<Void, ? super Throwable> action = (value, exception) -> {
            // this will throw an exception if an exception has been thrown by
            // the waitFor task
            waitFor.getNow(null);
            runOnFutureComplete();
        };

        devServerStartFuture = waitFor.whenCompleteAsync(action);

    }

    private void runOnFutureComplete() {
        try {
            doStartDevModeServer();
        } catch (ExecutionFailedException exception) {
            getLogger().error(null, exception);
            throw new CompletionException(exception);
        }
    }

    private void doStartDevModeServer() throws ExecutionFailedException {
        // If port is defined, means that webpack is already running
        if (port > 0) {
            if (!checkConnection()) {
                throw new IllegalStateException(String.format(
                        "%s webpack-dev-server port '%d' is defined but it's not working properly",
                        START_FAILURE, port));
            }
            reuseExistingPort(port);
            return;
        }
        port = getRunningDevServerPort(npmFolder);
        if (port > 0) {
            if (checkConnection()) {
                reuseExistingPort(port);
                return;
            } else {
                getLogger().warn(
                        "webpack-dev-server port '%d' is defined but it's not working properly. Using a new free port...",
                        port);
                port = 0;
            }
        }
        // here the port == 0
        validateFiles();

        long start = System.nanoTime();
        getLogger().info("Starting webpack-dev-server");

        watchDog.set(new DevServerWatchDog());

        // Look for a free port
        port = getFreePort();
        // save the port immediately before start a webpack server, see #8981
        saveRunningDevServerPort();

        try {
            Process process = doStartWebpack();
            webpackProcess.set(process);
            if (!isRunning()) {
                throw new IllegalStateException("Webpack exited prematurely");
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().info("Started webpack-dev-server. Time: {}ms", ms);
        } finally {
            if (webpackProcess.get() == null) {
                removeRunningDevServerPort();
            }
        }
    }

    /**
     * Validates that the needed server binary and config file(s) are available.
     * 
     * @throws ExecutionFailedException
     *             if there is a problem
     */
    protected void validateFiles() throws ExecutionFailedException {
        assert getPort() == 0;
        // Skip checks if we have a webpack-dev-server already running
        File webpack = getServerBinary();
        File webpackConfig = getServerConfig();
        if (!getNpmFolder().exists()) {
            getLogger().warn("No project folder '{}' exists", getNpmFolder());
            throw new ExecutionFailedException(START_FAILURE
                    + " the target execution folder doesn't exist.");
        }
        if (!webpack.exists()) {
            getLogger().warn("'{}' doesn't exist. Did you run `npm install`?",
                    webpack);
            throw new ExecutionFailedException(String.format(
                    "%s '%s' doesn't exist. `npm install` has not run or failed.",
                    START_FAILURE, webpack));
        } else if (!webpack.canExecute()) {
            getLogger().warn(
                    " '{}' is not an executable. Did you run `npm install`?",
                    webpack);
            throw new ExecutionFailedException(String.format(
                    "%s '%s' is not an executable."
                            + " `npm install` has not run or failed.",
                    START_FAILURE, webpack));
        }
        if (!webpackConfig.canRead()) {
            getLogger().warn(
                    "Webpack configuration '{}' is not found or is not readable.",
                    webpackConfig);
            throw new ExecutionFailedException(
                    String.format("%s '%s' doesn't exist or is not readable.",
                            START_FAILURE, webpackConfig));
        }
    }

    /**
     * Gets the binary that starts the dev server.
     */
    protected abstract File getServerBinary();

    /**
     * Gets the main configuration file for the dev server.
     */
    protected abstract File getServerConfig();

    /**
     * Starts the dev server and returns the started process.
     * 
     * @return the started process or {@code null} if no process was started
     */
    protected abstract Process doStartWebpack();

    /**
     * Gets the server watch dog.
     * 
     * @return the watch dog
     */
    protected DevServerWatchDog getWatchDog() {
        return watchDog.get();
    }

    /** Triggers live reload. */
    protected void triggerLiveReload() {
        if (liveReload != null) {
            liveReload.reload();
        }
    }

    /**
     * Gets the project root folder.
     * 
     * @return the project root folder
     */
    protected File getNpmFolder() {
        return npmFolder;
    }

    /**
     * Gets the application configuration.
     * 
     * @return the application configuration
     */
    protected ApplicationConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    /**
     * Check the connection to the dev server.
     * 
     * @return {@code true} if the dev server is responding correctly,
     *         {@code false} otherwise
     */
    protected abstract boolean checkConnection();

    private static int getRunningDevServerPort(File npmFolder) {
        int port = 0;
        File portFile = getDevServerPortFile(npmFolder);
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
     * Remove the running port from the vaadinContext and temporary file.
     */
    private void removeRunningDevServerPort() {
        FileUtils.deleteQuietly(devServerPortFile);
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

    private void reuseExistingPort(int port) {
        getLogger().info("Reusing webpack-dev-server running at {}:{}",
                WEBPACK_HOST, port);
        this.usingAlreadyStartedProcess = true;

        // Save running port for next usage
        saveRunningDevServerPort();
        watchDog.set(null);
    }

    private void saveRunningDevServerPort() {
        try {
            FileUtils.writeStringToFile(devServerPortFile, String.valueOf(port),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static File getDevServerPortFile(File npmFolder) {
        // UUID changes between JVM restarts
        String jvmUuid = System.getProperty(WEBPACK_PORTFILE_UUID_PROPERTY);
        if (jvmUuid == null) {
            jvmUuid = UUID.randomUUID().toString();
            System.setProperty(WEBPACK_PORTFILE_UUID_PROPERTY, jvmUuid);
        }

        // Frontend path ensures uniqueness for multiple devmode apps running
        // simultaneously
        String frontendBuildPath = npmFolder.getAbsolutePath();

        String uniqueUid = UUID.nameUUIDFromBytes(
                (jvmUuid + frontendBuildPath).getBytes(StandardCharsets.UTF_8))
                .toString();
        return new File(System.getProperty("java.io.tmpdir"), uniqueUid);
    }

    /**
     * Waits for the dev server to start.
     * <p>
     * Suspends the caller's thread until the dev mode server is started (or
     * failed to start).
     */
    public void waitForDevServer() {
        devServerStartFuture.join();
    }

    boolean isRunning() {
        Process process = webpackProcess.get();
        return (process != null && process.isAlive())
                || usingAlreadyStartedProcess;
    }

    @Override
    public void stop() {
        if (reuseDevServer) {
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

        webpackProcess.set(null);
        usingAlreadyStartedProcess = false;
        removeRunningDevServerPort();
    }

    @Override
    public HttpURLConnection prepareConnection(String path, String method)
            throws IOException {
        // path should have been checked at this point for any outside requests
        URL uri = new URL(WEBPACK_HOST + ":" + getPort() + path);
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod(method);
        connection.setReadTimeout(DEFAULT_TIMEOUT);
        connection.setConnectTimeout(DEFAULT_TIMEOUT);
        return connection;
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (devServerStartFuture.isDone()) {
            try {
                devServerStartFuture.getNow(null);
            } catch (CompletionException exception) {
                isDevServerFailedToStart.set(true);
                throw getCause(exception);
            }
            return false;
        } else {
            InputStream inputStream = WebpackHandler.class
                    .getResourceAsStream("dev-mode-not-ready.html");
            IOUtils.copy(inputStream, response.getOutputStream());
            response.setContentType("text/html;charset=utf-8");
            return true;
        }
    }

    @Override
    public boolean isDevModeRequest(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null
                && (pathInfo.startsWith("/" + Constants.VAADIN_MAPPING)
                        || StaticFileServer.APP_THEME_PATTERN.matcher(pathInfo)
                                .find())
                && !pathInfo.startsWith(
                        "/" + StreamRequestHandler.DYN_RES_PREFIX)) {
            return true;
        }
        return false;
    }

    /**
     * Serve a file by proxying to webpack.
     * <p>
     * Note: it considers the {@link HttpServletRequest#getPathInfo} that will
     * be the path passed to the 'webpack-dev-server' which is running in the
     * context root folder of the application.
     * <p>
     * Method returns {@code false} immediately if dev server failed on its
     * startup.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return false if webpack returned a not found, true otherwise
     * @throws IOException
     *             in the case something went wrong like connection refused
     */
    @Override
    public boolean serveDevModeRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // Do not serve requests if dev server starting or failed to start.
        if (isDevServerFailedToStart.get() || !devServerStartFuture.isDone()) {
            return false;
        }
        // Since we have 'publicPath=/VAADIN/' in webpack config,
        // a valid request for webpack-dev-server should start with '/VAADIN/'
        String requestFilename = request.getPathInfo();

        if (HandlerHelper.isPathUnsafe(requestFilename)
                || WEBPACK_ILLEGAL_CHAR_PATTERN.matcher(requestFilename)
                        .find()) {
            getLogger().info("Blocked attempt to access file: {}",
                    requestFilename);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return true;
        }

        // Redirect theme source request
        if (StaticFileServer.APP_THEME_PATTERN.matcher(requestFilename)
                .find()) {
            requestFilename = "/VAADIN/static" + requestFilename;
        }

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
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
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
                if ("Transfer-Encoding".equals(header)) {
                    return;
                }
                response.addHeader(header, values.get(0));
            }
        });

        if (responseCode == HttpURLConnection.HTTP_OK) {
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

    private RuntimeException getCause(Throwable exception) {
        if (exception instanceof CompletionException) {
            return getCause(exception.getCause());
        } else if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        } else {
            return new IllegalStateException(exception);
        }
    }

    protected void writeStream(ServletOutputStream outputStream,
            InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        while ((bytes = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytes);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(AbstractDevServerRunner.class);
    }

}
