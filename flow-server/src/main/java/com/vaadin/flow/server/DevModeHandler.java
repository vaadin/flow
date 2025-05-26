/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.StaticFileServer.APP_THEME_PATTERN;
import static com.vaadin.flow.server.frontend.FrontendUtils.GREEN;
import static com.vaadin.flow.server.frontend.FrontendUtils.RED;
import static com.vaadin.flow.server.frontend.FrontendUtils.YELLOW;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.FrontendUtils.console;
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public final class DevModeHandler implements RequestHandler {

    private static final String START_FAILURE = "Couldn't start dev server because";

    private static final AtomicReference<DevModeHandler> atomicHandler = new AtomicReference<>();

    // webpack dev-server allows " character if passed through, need to
    // explicitly check requests for it
    private static final Pattern WEBPACK_ILLEGAL_CHAR_PATTERN = Pattern
            .compile("\"|%22");
    // It's not possible to know whether webpack is ready unless reading output
    // messages. When webpack finishes, it writes either a `compiled` or a
    // `compiled with error` in the last line
    // Webpack output can be
    // webpack 5.51.1 compiled successfully in 27409ms
    // webpack 5.51.1 compiled with 34 errors in 42936ms
    // webpack 5.51.1 compiled with 2 warnings in 1233ms
    // webpack 5.51.1 compiled with 1 error and 1 warning in 7110 ms
    private static final String DEFAULT_OUTPUT_PATTERN = "webpack .* compiled .* in .*ms";
    private static final String DEFAULT_ERROR_PATTERN = "webpack .* compiled with .* error.* in .* ms";
    private static final String FAILED_MSG = "\n------------------ Frontend compilation failed. -----------------";
    private static final String SUCCEED_MSG = "\n----------------- Frontend compiled successfully. -----------------";
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

    private StringBuilder cumulativeOutput = new StringBuilder();

    private volatile String failedOutput;

    private AtomicBoolean isDevServerFailedToStart = new AtomicBoolean();

    private transient BrowserLiveReload liveReload;

    /**
     * The local installation path of the webpack-dev-server node script.
     */
    public static final String WEBPACK_SERVER = "node_modules/webpack-dev-server/bin/webpack-dev-server.js";

    private volatile int port;
    private final AtomicReference<Process> webpackProcess = new AtomicReference<>();
    private final boolean reuseDevServer;
    private final AtomicReference<DevServerWatchDog> watchDog = new AtomicReference<>();

    private final CompletableFuture<Void> devServerStartFuture;

    private final File npmFolder;

    private DevModeHandler(DeploymentConfiguration config, int runningPort,
            File npmFolder, CompletableFuture<Void> waitFor) {
        this.npmFolder = Objects.requireNonNull(npmFolder);

        port = runningPort;
        reuseDevServer = config.reuseDevServer();

        // Check whether executor is provided by the caller (framework)
        Object service = config.getInitParameters().get(Executor.class);

        BiConsumer<Void, ? super Throwable> action = (value, exception) -> {
            // this will throw an exception if an exception has been thrown by
            // the waitFor task
            waitFor.getNow(null);
            runOnFutureComplete(config);
        };

        if (service instanceof Executor) {
            // if there is an executor use it to run the task
            devServerStartFuture = waitFor.whenCompleteAsync(action,
                    (Executor) service);
        } else {
            devServerStartFuture = waitFor.whenCompleteAsync(action);
        }
    }

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param configuration
     *            deployment configuration
     * @param npmFolder
     *            folder with npm configuration files
     * @param waitFor
     *            a completable future whose execution result needs to be
     *            available to start the webpack dev server
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
     * @param waitFor
     *            a completable future whose execution result needs to be
     *            available to start the webpack dev server
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
        DevModeHandler handler = atomicHandler.get();
        if (handler == null) {
            handler = createInstance(runningPort, configuration, npmFolder,
                    waitFor);
            atomicHandler.compareAndSet(null, handler);
        }

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
        if (devServerStartFuture.isDone()) {
            try {
                devServerStartFuture.getNow(null);
            } catch (CompletionException exception) {
                isDevServerFailedToStart.set(true);
                throw getCause(exception);
            }
            return false;
        } else {
            try (InputStream inputStream = DevModeHandler.class
                    .getResourceAsStream("dev-mode-not-ready.html")) {
                IOUtils.copy(inputStream, response.getOutputStream());
            }
            response.setContentType("text/html;charset=utf-8");
            return true;
        }
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

    /**
     * Set the live reload service instance.
     *
     * @param liveReload
     *            the live reload instance
     */
    public void setLiveReload(BrowserLiveReload liveReload) {
        this.liveReload = liveReload;
    }

    /**
     * Get the live reload service instance.
     *
     * @return the live reload instance
     */
    public BrowserLiveReload getLiveReload() {
        return liveReload;
    }

    private static DevModeHandler createInstance(int runningPort,
            DeploymentConfiguration configuration, File npmFolder,
            CompletableFuture<Void> waitFor) {

        return new DevModeHandler(configuration, runningPort, npmFolder,
                waitFor);
    }

    /**
     * Returns true if it's a request that should be handled by webpack.
     *
     * @param request
     *            the servlet request
     * @return true if the request should be forwarded to webpack
     */
    public boolean isDevModeRequest(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo != null
                && (pathInfo.startsWith("/" + VAADIN_MAPPING)
                        || APP_THEME_PATTERN.matcher(pathInfo).find())
                && !pathInfo
                        .startsWith("/" + StreamRequestHandler.DYN_RES_PREFIX);
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
        if (APP_THEME_PATTERN.matcher(requestFilename).find()) {
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
                if ("Transfer-Encoding".equals(header)) {
                    return;
                }
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
        // path should have been checked at this point for any outside requests
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
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
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
            BufferedReader reader) throws IOException {
        StringBuilder output = getOutputBuilder();

        Consumer<String> info = s -> getLogger()
                .debug(String.format(GREEN, "{}"), s);
        Consumer<String> error = s -> getLogger()
                .error(String.format(RED, "{}"), s);
        Consumer<String> warn = s -> getLogger()
                .debug(String.format(YELLOW, "{}"), s);
        Consumer<String> log = info;
        for (String line; ((line = reader.readLine()) != null);) {
            String cleanLine = line
                    // remove color escape codes for console
                    .replaceAll("\u001b\\[[;\\d]*m", "")
                    // remove babel query string which is confusing
                    .replaceAll("\\?babel-target=[\\w\\d]+", "");

            // write each line read to logger, but selecting its correct level
            log = line.contains("WARNING") ? warn
                    : line.contains("ERROR") ? error
                            : isInfo(line, cleanLine) ? info : log;
            log.accept(cleanLine);

            // Only store webpack errors to be shown in the browser.
            if (log.equals(error)) {
                // save output so as it can be used to alert user in browser.
                output.append(cleanLine).append(System.lineSeparator());
            }

            // save output so as it can be used to log exception if run fails
            cumulativeOutput.append(cleanLine).append(System.lineSeparator());

            boolean succeed = success.matcher(line).find();
            boolean failed = failure.matcher(line).find();
            // We found the success or failure pattern in stream
            if (succeed || failed) {
                log.accept(succeed ? SUCCEED_MSG : FAILED_MSG);
                // save output in case of failure
                failedOutput = failed ? output.toString() : null;
                // reset output and logger for the next compilation
                output = getOutputBuilder();
                cumulativeOutput = new StringBuilder();
                log = info;
                // Notify DevModeHandler to continue
                doNotify();
            }
        }
    }

    private boolean isInfo(String line, String cleanLine) {
        return line.trim().isEmpty() || cleanLine.trim().startsWith("i");
    }

    private StringBuilder getOutputBuilder() {
        StringBuilder output = new StringBuilder();
        output.append(String.format("Webpack build failed with errors:%n"));
        return output;
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
        // Using an short prefix so as webpack output is more readable
        return LoggerFactory.getLogger("dev-webpack");
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

    private void runOnFutureComplete(DeploymentConfiguration config) {
        try {
            doStartDevModeServer(config);
        } catch (ExecutionFailedException exception) {
            getLogger().error(null, exception);
            throw new CompletionException(exception);
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

    private void doStartDevModeServer(DeploymentConfiguration config)
            throws ExecutionFailedException {
        // If port is defined, means that webpack is already running
        if (port > 0) {
            if (!checkWebpackConnection()) {
                throw new IllegalStateException(String.format(
                        "%s webpack-dev-server port '%d' is defined but it's not working properly",
                        START_FAILURE, port));
            }
            reuseExistingPort(port);
            return;
        }
        port = getRunningDevServerPort();
        if (port > 0) {
            if (checkWebpackConnection()) {
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
        Pair<File, File> webPackFiles = validateFiles(npmFolder);

        getLogger().info("Starting webpack-dev-server");

        watchDog.set(new DevServerWatchDog());

        // Look for a free port
        port = getFreePort();
        saveRunningDevServerPort();
        boolean success = false;
        try {
            success = doStartWebpack(config, webPackFiles);
        } finally {
            if (!success) {
                removeRunningDevServerPort();
            }
        }
    }

    private boolean doStartWebpack(DeploymentConfiguration config,
            Pair<File, File> webPackFiles) {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(npmFolder);

        final String nodeVersion = config.getStringProperty(NODE_VERSION,
                FrontendTools.DEFAULT_NODE_VERSION);
        final String nodeDownloadRoot = config.getStringProperty(
                NODE_DOWNLOAD_ROOT, NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);
        boolean useHomeNodeExec = config.getBooleanProperty(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, false);
        FrontendTools tools = new FrontendTools(npmFolder.getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath(),
                nodeVersion, URI.create(nodeDownloadRoot), useHomeNodeExec);
        tools.validateNodeAndNpmVersion();
        processBuilder.environment().putAll(tools.getWebpackNodeEnvironment());

        String nodeExec = null;
        if (useHomeNodeExec) {
            nodeExec = tools.forceAlternativeNodeExecutable();
        } else {
            nodeExec = tools.getNodeExecutable();
        }

        List<String> command = makeCommands(config, webPackFiles.getFirst(),
                webPackFiles.getSecond(), nodeExec);

        console(GREEN, START);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(
                    commandToString(npmFolder.getAbsolutePath(), command));
        }

        long start = System.nanoTime();

        processBuilder.command(command);
        processBuilder.environment().put("watchDogPort",
                Integer.toString(watchDog.get().getWatchDogPort()));
        try {
            webpackProcess.set(
                    processBuilder.redirectError(ProcessBuilder.Redirect.PIPE)
                            .redirectErrorStream(true).start());

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

            logStream(webpackProcess.get().getInputStream(), succeed, failure);

            getLogger().info(LOG_START);
            synchronized (this) {
                this.wait(Integer.parseInt(config.getStringProperty( // NOSONAR
                        SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT,
                        DEFAULT_TIMEOUT_FOR_PATTERN)));
            }
            if (!webpackProcess.get().isAlive()) {
                getLogger().error("Webpack failed ({}) with the exception:\n{}",
                        webpackProcess.get().exitValue(),
                        cumulativeOutput.toString());
                throw new IllegalStateException("Webpack exited prematurely");
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().info(LOG_END, ms);
            saveRunningDevServerPort();
            return true;
        } catch (IOException e) {
            getLogger().error("Failed to start the webpack process", e);
        } catch (InterruptedException e) {
            getLogger().debug("Webpack process start has been interrupted", e);
        }
        return false;
    }

    private void reuseExistingPort(int port) {
        getLogger().info("Reusing webpack-dev-server running at {}:{}",
                WEBPACK_HOST, port);

        // Save running port for next usage
        saveRunningDevServerPort();
        watchDog.set(null);
    }

    private List<String> makeCommands(DeploymentConfiguration config,
            File webpack, File webpackConfig, String nodeExec) {
        List<String> command = new ArrayList<>();
        command.add(nodeExec);
        command.add(webpack.getAbsolutePath());
        command.add("--config");
        command.add(webpackConfig.getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(port));
        // Tell wds to stop even if watchDog fail
        command.add("--watch-options-stdin");

        command.addAll(Arrays.asList(config
                .getStringProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
                        "--devtool=eval-source-map --mode=development")
                .split(" +")));
        return command;
    }

    private Pair<File, File> validateFiles(File npmFolder)
            throws ExecutionFailedException {
        assert port == 0;
        // Skip checks if we have a webpack-dev-server already running
        File webpack = new File(npmFolder, WEBPACK_SERVER);
        File webpackConfig = new File(npmFolder, FrontendUtils.WEBPACK_CONFIG);
        if (!npmFolder.exists()) {
            getLogger().warn("No project folder '{}' exists", npmFolder);
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
        return new Pair<>(webpack, webpackConfig);
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

    /**
     * Waits for the dev server to start.
     * <p>
     * Suspends the caller's thread until the dev mode server is started (or
     * failed to start).
     *
     * @see Thread#join()
     */
    void join() {
        devServerStartFuture.join();
    }

    private static final class LazyDevServerPortFileInit {

        private static final File DEV_SERVER_PORT_FILE = createDevServerPortFile();

        private static File createDevServerPortFile() {
            try {
                return File.createTempFile("flow-dev-server", "port");
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

    }

}
