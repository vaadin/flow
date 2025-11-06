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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.DevServerOutputTracker.Result;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.NetworkUtil;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import java.nio.file.Files;

/**
 * Deals with most details of starting a frontend development server or
 * connecting to an existing one.
 * <p>
 * This class is meant to be used during developing time.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class AbstractDevServerRunner implements DevModeHandler {

    private static final String START_FAILURE = "Couldn't start dev server because";

    public static final String DEV_SERVER_HOST = "http://127.0.0.1";

    private static final String FAILED_MSG = "\n------------------ Frontend compilation failed. ------------------\n\n";
    private static final String SUCCEED_MSG = "\n----------------- Frontend compiled successfully. -----------------\n\n";
    private static final String START = "\n------------------ Starting Frontend compilation. ------------------\n";
    private static final String LOG_START = "Running {} to compile frontend resources. This may take a moment, please stand by...";

    /**
     * If after this time in millisecs, the pattern was not found, we unlock the
     * process and continue. It might happen if the dev server changes their
     * output.
     */
    private static final String DEFAULT_TIMEOUT_FOR_PATTERN = "60000";

    /**
     * UUID system property for identifying JVM restart.
     */
    private static final String DEV_SERVER_PORTFILE_UUID_PROPERTY = "vaadin.frontend.devserver.portfile.uuid";

    // webpack dev-server allows " character if passed through, need to
    // explicitly check requests for it
    private static final Pattern WEBPACK_ILLEGAL_CHAR_PATTERN = Pattern
            .compile("\"|%22");

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private static final int DEFAULT_TIMEOUT = 120 * 1000;

    private final File npmFolder;
    private volatile int port;
    private final AtomicReference<Process> devServerProcess = new AtomicReference<>();
    private final boolean reuseDevServer;
    private final File devServerPortFile;

    private AtomicBoolean isDevServerFailedToStart = new AtomicBoolean();

    private final CompletableFuture<Void> devServerStartFuture;

    private final AtomicReference<DevServerWatchDog> watchDog = new AtomicReference<>();

    private boolean usingAlreadyStartedProcess = false;

    private ApplicationConfiguration applicationConfiguration;

    private FrontendTools frontendTools;

    private String failedOutput = null;

    private transient Runnable waitForRestart;

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
        frontendTools = new FrontendTools(applicationConfiguration, npmFolder);
        devServerPortFile = getDevServerPortFile(npmFolder);

        BiConsumer<Void, ? super Throwable> action = (value, exception) -> {
            // this will throw an exception if an exception has been thrown by
            // the waitFor task
            waitFor.getNow(null);
            runOnFutureComplete();
        };

        devServerStartFuture = waitFor.whenCompleteAsync(action);

    }

    protected FrontendTools getFrontendTools() {
        return frontendTools;
    }

    private void runOnFutureComplete() {
        try {
            doStartDevModeServer();
        } catch (ExecutionFailedException exception) {
            getLogger().error(null, exception);
            throw new CompletionException(exception);
        }
    }

    void doStartDevModeServer() throws ExecutionFailedException {
        waitForRestart = DevServerOutputTracker.activeServerRestartGuard();
        if (waitForRestart != null) {
            getLogger().debug("RestartMonitor is active");
        }
        // If port is defined, means that the dev server is already running
        if (port > 0) {
            if (!checkConnection()) {
                throw new IllegalStateException(String.format(
                        "%s %s port '%d' is defined but it's not working properly",
                        getServerName(), START_FAILURE, port));
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
                getLogger().warn(String.format(
                        "%s port '%d' is defined but it's not working properly. Using a new free port...",
                        getServerName(), port));
                port = 0;
            }
        }
        // here the port == 0
        validateFiles();

        long start = System.nanoTime();
        getLogger().info("Starting " + getServerName());

        watchDog.set(new DevServerWatchDog());

        // Look for a free port
        port = NetworkUtil.getFreePort();
        // save the port immediately before start a dev server, see #8981
        saveRunningDevServerPort();

        try {
            Process process = doStartDevServer();
            devServerProcess.set(process);
            if (!isRunning()) {
                throw new IllegalStateException("Startup of " + getServerName()
                        + " failed. Output was:\n" + getFailedOutput());
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().info("Started {}. Time: {}ms", getServerName(), ms);
        } finally {
            if (devServerProcess.get() == null) {
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
        // Skip checks if we have a dev server already running
        File binary = getServerBinary();
        File config = getServerConfig();
        if (!getProjectRoot().exists()) {
            getLogger().warn("No project folder '{}' exists", getProjectRoot());
            throw new ExecutionFailedException(START_FAILURE
                    + " the target execution folder doesn't exist.");
        }
        if (!binary.exists()) {
            getLogger().warn("'{}' doesn't exist. Did you run `npm install`?",
                    binary);
            throw new ExecutionFailedException(String.format(
                    "%s '%s' doesn't exist. `npm install` has not run or failed.",
                    START_FAILURE, binary));
        } else if (!binary.canExecute()) {
            getLogger().warn(
                    " '{}' is not an executable. Did you run `npm install`?",
                    binary);
            throw new ExecutionFailedException(String.format(
                    "%s '%s' is not an executable."
                            + " `npm install` has not run or failed.",
                    START_FAILURE, binary));
        }
        if (!config.canRead()) {
            getLogger().warn(
                    "{} configuration '{}' is not found or is not readable.",
                    getServerName(), config);
            throw new ExecutionFailedException(
                    String.format("%s '%s' doesn't exist or is not readable.",
                            START_FAILURE, config));
        }
    }

    /**
     * Gets the binary that starts the dev server.
     *
     * @return the dev server binary file
     */
    protected abstract File getServerBinary();

    /**
     * Gets the main configuration file for the dev server.
     *
     * @return the dev server configuration file
     */
    protected abstract File getServerConfig();

    /**
     * Gets the name of the dev server for outputting to the user and
     * statistics.
     *
     * @return the dev server name
     */
    protected abstract String getServerName();

    /**
     * Gets the commands to run to start the dev server.
     *
     * @param tools
     *            the frontend tools object
     * @return the list of commands to start the dev server
     */
    protected abstract List<String> getServerStartupCommand(
            FrontendTools tools);

    /**
     * Defines the environment variables to use when starting the dev server.
     *
     * @param frontendTools
     *            frontend tools metadata
     * @param environment
     *            the environment variables to use
     */
    protected void updateServerStartupEnvironment(FrontendTools frontendTools,
            Map<String, String> environment) {
        environment.put("watchDogHost", getLoopbackAddress().getHostAddress());
        environment.put("watchDogPort",
                Integer.toString(getWatchDog().getWatchDogPort()));
    }

    // visible for tests
    InetAddress getLoopbackAddress() {
        return InetAddress.getLoopbackAddress();
    }

    /**
     * Gets a pattern to match with the output to determine that the server has
     * started successfully.
     *
     * @return the success pattern
     */
    protected abstract Pattern getServerSuccessPattern();

    /**
     * Gets a pattern to match with the output to determine that the server has
     * failed to start.
     *
     * @return the failure pattern
     */
    protected abstract Pattern getServerFailurePattern();

    /**
     * Gets a pattern to match with the output to determine that the server is
     * restarting.
     *
     * Defaults to {@literal null}, meaning that server restart is not
     * monitored.
     *
     * Server restart is monitored only if both this method and
     * {@link #getServerRestartedPattern()} provides a pattern.
     *
     * @return the restarting pattern, or {@code null} if restart monitoring is
     *         not used
     */
    protected Pattern getServerRestartingPattern() {
        return null;
    }

    /**
     * Gets a pattern to match with the output to determine that the server has
     * been restarted.
     *
     * Defaults to {@literal null}, meaning that server restart is not
     * monitored.
     *
     * Server restart is monitored only if both this method and
     * {@link #getServerRestartingPattern()} provides a pattern.
     *
     * @return the restarted pattern, or {@code null} if restart monitoring is
     *         not used
     */
    protected Pattern getServerRestartedPattern() {
        return null;
    }

    /**
     * Starts the dev server and returns the started process.
     *
     * @return the started process or {@code null} if no process was started
     */
    protected Process doStartDevServer() {
        ApplicationConfiguration config = getApplicationConfiguration();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(getProjectRoot());
        frontendTools.validateNodeAndNpmVersion();

        List<String> command = getServerStartupCommand(frontendTools);

        FrontendUtils.console(FrontendUtils.GREEN, START);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(FrontendUtils.commandToString(
                    getProjectRoot().getAbsolutePath(), command));
        }

        processBuilder.command(command);

        Map<String, String> environment = processBuilder.environment();
        updateServerStartupEnvironment(frontendTools, environment);

        try {
            Process process = processBuilder.redirectErrorStream(true).start();
            /*
             * We only can save the dev server process reference the first time
             * that the DevModeHandler is created. There is no way to store it
             * in the servlet container, and we do not want to save it in the
             * global JVM.
             *
             * We instruct the JVM to stop the server daemon when the JVM stops,
             * to avoid leaving daemons running in the system.
             *
             * NOTE: that in the corner case that the JVM crashes or it is
             * killed the daemon will be kept running. But anyways it will also
             * happens if the system was configured to be stop the daemon when
             * the servlet context is destroyed.
             */
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            DevServerOutputTracker outputTracker = new DevServerOutputTracker(
                    process.getInputStream(), getServerSuccessPattern(),
                    getServerFailurePattern(), this::onDevServerCompilation);

            Pattern restartingPattern = getServerRestartingPattern();
            Pattern restartedPattern = getServerRestartedPattern();
            if (restartingPattern != null && restartedPattern != null) {
                waitForRestart = outputTracker.serverRestartGuard(
                        restartingPattern, restartedPattern);
                getLogger().debug("RestartMonitor is active");
            } else {
                getLogger().trace(
                        "RestartMonitor not active. Both restarting and restarted pattern are required");
            }

            outputTracker.find();
            getLogger().info(LOG_START, getServerName());

            int timeout = Integer.parseInt(config.getStringProperty(
                    InitParameters.SERVLET_PARAMETER_DEVMODE_TIMEOUT,
                    DEFAULT_TIMEOUT_FOR_PATTERN));
            outputTracker.awaitFirstMatch(timeout);

            return process;
        } catch (IOException e) {
            getLogger().error(
                    "Failed to start the " + getServerName() + " process", e);
        } catch (InterruptedException e) {
            getLogger().debug(
                    getServerName() + " process start has been interrupted", e);
        }
        return null;
    }

    /**
     * Called whenever the dev server output matche the success or failure
     * pattern.
     *
     * @param result
     *            the compilation result
     */
    protected void onDevServerCompilation(Result result) {
        if (result.isSuccess()) {
            FrontendUtils.console(FrontendUtils.GREEN, SUCCEED_MSG);
            failedOutput = null;
        } else {
            FrontendUtils.console(FrontendUtils.RED, FAILED_MSG);
            failedOutput = result.getOutput();
        }
    }

    @Override
    public String getFailedOutput() {
        return failedOutput;
    }

    /**
     * Gets the server watch dog.
     *
     * @return the watch dog
     */
    protected DevServerWatchDog getWatchDog() {
        return watchDog.get();
    }

    @Override
    public File getProjectRoot() {
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
    protected boolean checkConnection() {
        try {
            HttpURLConnection connection = prepareConnection("/index.html",
                    "GET");
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            getLogger().debug("Error checking dev server connection", e);
        }
        return false;
    }

    private static int getRunningDevServerPort(File npmFolder) {
        int port = 0;
        File portFile = getDevServerPortFile(npmFolder);
        if (portFile.canRead()) {
            try {
                String portString = Files.readString(portFile.toPath());
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
        if(devServerPortFile.exists()) {
            try {
                devServerPortFile.delete();
            } catch (Exception e) {
                // NOP
            }
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    private void reuseExistingPort(int port) {
        getLogger().info("Reusing {} running at {}:{}", getServerName(),
                DEV_SERVER_HOST, port);
        this.usingAlreadyStartedProcess = true;

        // Save running port for next usage
        saveRunningDevServerPort();
        watchDog.set(null);
    }

    private void saveRunningDevServerPort() {
        try {
            Files.writeString(devServerPortFile.toPath(), String.valueOf(port));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static File getDevServerPortFile(File npmFolder) {
        // UUID changes between JVM restarts
        String jvmUuid = System.getProperty(DEV_SERVER_PORTFILE_UUID_PROPERTY);
        if (jvmUuid == null) {
            jvmUuid = UUID.randomUUID().toString();
            System.setProperty(DEV_SERVER_PORTFILE_UUID_PROPERTY, jvmUuid);
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
        Process process = devServerProcess.get();
        return (process != null && process.isAlive())
                || usingAlreadyStartedProcess;
    }

    @Override
    public void stop() {
        if (reuseDevServer) {
            return;
        }

        try {
            // The most reliable way to stop the dev server is
            // by informing it to exit. We have implemented
            // a listener that handles the stop command via HTTP and exits.
            prepareConnection("/stop", "GET").getResponseCode();
        } catch (IOException e) {
            getLogger().debug(
                    getServerName() + " does not support the `/stop` command.",
                    e);
        }

        DevServerWatchDog watchDogInstance = watchDog.get();
        if (watchDogInstance != null) {
            watchDogInstance.stop();
        }

        Process process = devServerProcess.get();
        if (process != null && process.isAlive()) {
            process.destroy();
        }

        devServerProcess.set(null);
        usingAlreadyStartedProcess = false;
        removeRunningDevServerPort();
    }

    @Override
    public HttpURLConnection prepareConnection(String path, String method)
            throws IOException {
        if (waitForRestart != null) {
            waitForRestart.run();
        }
        // path should have been checked at this point for any outside requests
        URL uri = new URL(DEV_SERVER_HOST + ":" + getPort() + path);
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod(method);
        connection.setReadTimeout(DEFAULT_TIMEOUT);
        connection.setConnectTimeout(DEFAULT_TIMEOUT);
        return connection;
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        return handleRequestInternal(session, request, response,
                devServerStartFuture, isDevServerFailedToStart);
    }

    static boolean handleRequestInternal(VaadinSession session,
            VaadinRequest request, VaadinResponse response,
            CompletableFuture<?> devServerStartFuture,
            AtomicBoolean isDevServerFailedToStart) throws IOException {
        if (devServerStartFuture.isDone()) {
            // The server has started, check for any exceptions in the startup
            // process
            try {
                devServerStartFuture.getNow(null);
            } catch (CompletionException exception) {
                isDevServerFailedToStart.set(true);
                throw getCause(exception);
            }
            if (request.getHeader("X-DevModePoll") != null) {
                // Avoid creating a UI that is thrown away for polling requests
                response.setContentType("text/html;charset=utf-8");
                response.getWriter().write("Ready");
                response.setHeader("Cache-Control", "no-cache");
                return true;
            }
            try {
                session.getLockInstance().lock();
                VaadinService service = session.getService();
                RouteUtil.checkForClientRouteCollisions(service, service
                        .getRouter().getRegistry().getRegisteredRoutes());
            } finally {
                session.getLockInstance().unlock();
            }

            return false;
        } else {
            if (request.getHeader("X-DevModePoll") == null) {
                // The initial request while the dev server is starting
                InputStream inputStream = AbstractDevServerRunner.class
                        .getResourceAsStream("dev-mode-not-ready.html");
                inputStream.transferTo(response.getOutputStream());
            } else {
                // A polling request while the server is starting
                response.getWriter().write("Pending");
            }
            response.setContentType("text/html;charset=utf-8");
            response.setHeader("X-DevModePending", "true");
            response.setHeader("Cache-Control", "no-cache");
            return true;
        }
    }

    /**
     * Serve a file by proxying to the dev server.
     * <p>
     * Note: it considers the {@link HttpServletRequest#getPathInfo} that will
     * be the path passed to the dev server which is running in the context root
     * folder of the application.
     * <p>
     * Method returns {@code false} immediately if dev server failed on its
     * startup.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return false if the dev server returned a not found, true otherwise
     * @throws IOException
     *             in the case something went wrong like connection refused
     */
    @Override
    public boolean serveDevModeRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // Do not serve requests if dev server starting or failed to start.
        if (isDevServerFailedToStart.get() || !devServerStartFuture.isDone()
                || devServerStartFuture.isCompletedExceptionally()) {
            return false;
        }
        // Since we have 'publicPath=/VAADIN/' in the dev server config,
        // a valid request for the dev server should start with '/VAADIN/'
        String requestFilename = request.getPathInfo();

        if (HandlerHelper.isPathUnsafe(requestFilename)
                || WEBPACK_ILLEGAL_CHAR_PATTERN.matcher(requestFilename)
                        .find()) {
            getLogger().info("Blocked attempt to access file: {}",
                    requestFilename);
            response.setStatus(HttpStatusCode.FORBIDDEN.getCode());
            return true;
        }

        // Redirect theme source request
        if (StaticFileServer.APP_THEME_ASSETS_PATTERN.matcher(requestFilename)
                .find()) {
            requestFilename = "/VAADIN/static" + requestFilename;
        }

        if (requestFilename.equals("") || requestFilename.equals("/")) {
            // Index file must be handled by IndexHtmlRequestHandler
            return false;
        }
        String devServerRequestPath = UrlUtil.encodeURI(requestFilename);
        if (request.getQueryString() != null) {
            devServerRequestPath += "?" + request.getQueryString();
        }
        HttpURLConnection connection = prepareConnection(devServerRequestPath,
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
        getLogger().debug("Requesting resource from {} {}", getServerName(),
                connection.getURL());
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            getLogger().debug("Resource not served by {} {}", getServerName(),
                    devServerRequestPath);
            // the dev server cannot access the resource, return false so Flow
            // can
            // handle it
            return false;
        }
        getLogger().debug("Served resource by {}: {} {}", getServerName(),
                responseCode, devServerRequestPath);

        // Copies response headers
        connection.getHeaderFields().forEach((header, values) -> {
            if (header != null) {
                if ("Transfer-Encoding".equals(header)) {
                    return;
                }
                response.addHeader(header, values.get(0));
            }
        });

        if (requestFilename
                .startsWith("/VAADIN/generated/jar-resources/copilot/")) {
            // Cache copilot files as they have a generated hash at the end
            response.setHeader("Cache-Control", "max-age=31536001,immutable");
        }
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

    private static RuntimeException getCause(Throwable exception) {
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
