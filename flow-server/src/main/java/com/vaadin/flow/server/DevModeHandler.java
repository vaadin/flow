/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;
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
 */
public class DevModeHandler implements Serializable {

    // Non final because tests need to reset this during teardown.
    private static AtomicReference<DevModeHandler> atomicHandler = new AtomicReference<>();

    // It's not possible to know whether webpack is ready unless reading output messages.
    // When webpack finishes, it writes either a `Compiled` or `Failed` as the last line
    private static final String DEFAULT_OUTPUT_PATTERN = ": Compiled.";
    private static final String DEFAULT_ERROR_PATTERN = ": Failed to compile.";

    // If after this time in millisecs, the pattern was not found, we unlock the process
    // and continue. It might happen if webpack changes their output without advise.
    private static final String DEFAULT_TIMEOUT_FOR_PATTERN = "60000";

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private static final int DEFAULT_TIMEOUT = 120 * 1000;
    private static final String WEBPACK_HOST = "http://localhost";

    private boolean notified = false;

    private static String failedOutput;

    /**
     * The local installation path of the webpack-dev-server node script.
     */
    public static final String WEBPACK_SERVER = "node_modules/webpack-dev-server/bin/webpack-dev-server.js";

    private int port;
    private final DeploymentConfiguration config;

    // For testing purposes
    DevModeHandler(DeploymentConfiguration configuration, int port) {
        this.config = configuration;
        this.port = port;
    }

    private DevModeHandler(DeploymentConfiguration configuration,
            File directory, File webpack, File webpackConfig) {
        this.config = configuration;

        // If port is defined, means that webpack is already running
        port = Integer.parseInt(config.getStringProperty(
                SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT, "0"));
        if (port > 0 && checkWebpackConnection()) {
            getLogger().info("Webpack is running at {}:{}", WEBPACK_HOST, port);
            return;
        }

        // We always compute a free port.
        port = getFreePort();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(directory);

        List<String> command = new ArrayList<>();
        command.add(FrontendUtils.getNodeExecutable());
        command.add(webpack.getAbsolutePath());
        command.add("--config");
        command.add(webpackConfig.getAbsolutePath());
        command.add("--port");
        command.add(String.valueOf(port));
        command.addAll(Arrays.asList(config
                .getStringProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
                        "-d --hot false")
                .split(" +")));

        if (getLogger().isInfoEnabled()) {
            getLogger().info("Starting Webpack in dev mode\n {}",
                    String.join(" ", command));
        }

        processBuilder.command(command);
        try {
            Process webpackProcess = processBuilder
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectErrorStream(true).start();
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(webpackProcess::destroy));

            Pattern succeed = Pattern.compile(config.getStringProperty(
                    SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN,
                    DEFAULT_OUTPUT_PATTERN));

            Pattern failure = Pattern.compile(config.getStringProperty(
                    SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN,
                    DEFAULT_ERROR_PATTERN));

            logStream(webpackProcess.getInputStream(), succeed, failure);

            synchronized (this) {
                this.wait(Integer.parseInt(config.getStringProperty( // NOSONAR
                        SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT,
                        DEFAULT_TIMEOUT_FOR_PATTERN)));
            }

            if (!webpackProcess.isAlive()) {
                throw new IllegalStateException("Webpack exited prematurely");
            }
        } catch (IOException | InterruptedException e) {
            getLogger().error("Failed to start the webpack process", e);
        }

        System.setProperty(
                "vaadin." + SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT,
                String.valueOf(port));
    }

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param configuration
     *         deployment configuration
     *
     * @return the instance in case everything is alright, null otherwise
     */
    public static DevModeHandler start(DeploymentConfiguration configuration) {
        atomicHandler.compareAndSet(null,
                DevModeHandler.createInstance(configuration));
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

    private static DevModeHandler createInstance(DeploymentConfiguration configuration) {
        if (configuration.isBowerMode() || configuration.isProductionMode()) {
            getLogger().trace("Instance not created because not in npm-dev mode");
            return null;
        }

        File directory = new File(getBaseDir(), FrontendUtils.WEBAPP_FOLDER).getAbsoluteFile();
        if (!directory.exists()) {
            getLogger().warn("Instance not created because cannot change to '{}'", directory);
            return null;
        }

        File webpack = new File(getBaseDir(), WEBPACK_SERVER);
        if (!webpack.canExecute()) {
            getLogger().warn("Instance not created because cannot execute '{}'. Did you run `npm install`", webpack);
            return null;
        } else if(!webpack.exists()) {
            getLogger().warn("Instance not created because file '{}' doesn't exist. Did you run `npm install`",
                    webpack);
            return null;
        }

        File webpackConfig = new File(getBaseDir(), FrontendUtils.WEBPACK_CONFIG);
        if (!webpackConfig.canRead()) {
            getLogger().warn("Instance not created because there is not webpack configuration '{}'", webpackConfig);
            return null;
        }

        return new DevModeHandler(configuration, directory, webpack, webpackConfig);
    }

    /**
     * Returns true if it's a request that should be handled by webpack.
     *
     * @param request
     *            the servlet request
     * @return true if the request should be forwarded to webpack
     */
    public boolean isDevModeRequest(HttpServletRequest request) {
        return getRequestFilename(request).matches(".+\\.js");
    }

    /**
     * Serve a file by proxying to webpack.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return false if webpack returned a not found, true otherwise
     * @throws IOException
     *             in the case something went wrong like connection refused
     */
    public boolean serveDevModeRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestFilename = getRequestFilename(request);

        HttpURLConnection connection = prepareConnection(requestFilename, request.getMethod());

        // Copies all the headers from the original request
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            connection.setRequestProperty(header,
                    // Exclude keep-alive
                    "Connect".equals(header) ? "close" : request.getHeader(header));
        }

        // Send the request
        int responseCode = connection.getResponseCode();
        if (responseCode == HTTP_NOT_FOUND) {
            getLogger().debug("Resource not served by webpack {}", requestFilename);
            // webpack cannot access the resource, return false so as flow can
            // handle it
            return false;
        }

        getLogger().debug("Served resource by webpack: {} {}", responseCode, requestFilename);

        // Copies response headers
        connection.getHeaderFields().forEach((header, values) -> {
            if (header != null) {
                response.addHeader(header, values.get(0));
            }
        });

        if (responseCode == HTTP_OK) {
            // Copies response payload
            writeStream(response.getOutputStream(), connection.getInputStream());
        }

        // Copies response code
        response.sendError(responseCode);

        // Close request to avoid issues in CI and Chrome
        response.getOutputStream().close();

        return true;
    }

    private boolean checkWebpackConnection() {
        try {
            prepareConnection("/", "GET").getResponseCode();
            return true;
        } catch (IOException e) {
            getLogger().debug("Error checking webpack dev server connection", e);
        }
        return false;
    }

    private HttpURLConnection prepareConnection(String path, String method) throws IOException {
        URL uri = new URL(WEBPACK_HOST + ":" + port + path);
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod(method);
        connection.setReadTimeout(DEFAULT_TIMEOUT);
        connection.setConnectTimeout(DEFAULT_TIMEOUT);
        return connection;
    }

    private void doNotify() {
        if (!notified) {
            notified = true;
            synchronized (this) {
                notify();//NOSONAR
            }
        }
    }

    // mirrors a stream to logger, and check whether a pattern is found in the output.
    private void logStream(InputStream input, Pattern success, Pattern failure) {
        Thread thread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String output = "";
            try {
                for (String line; ((line = reader.readLine()) != null);) {
                    getLogger().info(line);
                    output += line
                            // remove escape codes for console colors
                            .replaceAll("\u001B\\[[;\\d]*m", "")
                            // babel query string for imports is confusing
                            .replaceAll("\\?babel-target=[^\"]+", "")
                            + "\n";

                    // We found the started pattern in stream, notify
                    // DevModeHandler to continue
                    if (success != null && success.matcher(line).find()) {
                        failedOutput = null;
                        output = "";
                        doNotify();
                    }

                    if (failure != null && failure.matcher(line).find()) {
                        failedOutput = output;
                        output = "";
                        doNotify();
                    }
                }
            } catch (IOException e) {
                getLogger().error("Exception when reading webpack output.", e);
            }

            // Process closed stream, means that it exited, notify
            // DevModeHandler to continue
            doNotify();
        });
        thread.setName("webpack");
        thread.start();
    }

    private void writeStream(ServletOutputStream outputStream, InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        while ((bytes = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytes);
        }
    }

    private String getRequestFilename(HttpServletRequest request) {
        return request.getPathInfo() == null ? request.getServletPath()
                : request.getServletPath() + request.getPathInfo();
    }

    private static Logger getLogger() {
        // Using short prefix so as webpack output is more readable
        return LoggerFactory.getLogger("dev-server");
    }

    /**
     * Return webpack console output when a compilation error happened.
     *
     * @return console output if error or null otherwise.
     */
    public static String getFailedOutput() {
        return failedOutput;
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
            throw new IllegalStateException("Unable to find a free port for running webpack", e);
        }
    }
}
