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

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

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
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;

/**
 * Handles getting resources from <code>webpack-dev-server</code>.
 * <p>
 * This class is meant to be used during developing time. For a production mode
 * site <code>webpack</code> generates the static bundles that will be served
 * directly from the servlet (using a default servlet if such exists) or through
 * a stand alone static file server.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class DevModeHandler implements Serializable {

    private static AtomicReference<DevModeHandler> devmodeHandler = new AtomicReference<>();

    public static final String PARAM_WEBPACK_RUNNING = "vaadin.devmode.webpack.running";
    static final String PARAM_WEBPACK_TIMEOUT = "vaadin.devmode.webpack.timeout";

    // It's not possible to know whether webpack is ready unless reading output messages.
    // When webpack finishes, it writes either a `Compiled` or `Failed` as the last line
    private static final Pattern OUTPUT_PATTERN = Pattern.compile(": (Compiled|Failed)");
    // If after this time in millisecs, the pattern was not found, we unlock the process
    // and continue. It might happen if webpack changes their output without advise.
    private static final int DEFAULT_TIMEOUT_FOR_PATTERN = 60000;

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private static final int DEFAULT_TIMEOUT = 120 * 1000;
    private static final String WEBPACK_HOST = "http://localhost";
    static final Boolean IS_UNIX = !System.getProperty("os.name").matches("(?i).*windows.*");

    // This fixes maven tests in multi-module execution
    static final String BASEDIR = System.getProperty("project.basedir", System.getProperty("user.dir", "."));
    static final String WEBAPP_FOLDER = BASEDIR + "./";
    static final String WEBPACK_CONFIG = BASEDIR + "/webpack.config.js";
    static final String WEBPACK_SERVER = BASEDIR + "/node_modules/webpack-dev-server/bin/webpack-dev-server.js";

    private int port;

    // For testing purposes
    DevModeHandler(int port) {
        this.port = port;
    }

    private DevModeHandler(File directory, File webpack, File webpackConfig) {

        port = Integer.getInteger(PARAM_WEBPACK_RUNNING, 0);
        if (port > 0 && checkWebpackConnection()) {
            getLogger().info("Webpack is running at {}:{}", WEBPACK_HOST, port);
            return;
        }

        port = getFreePort();

        getLogger().info("Starting Webpack in dev mode ...");
        ProcessBuilder process = new ProcessBuilder();
        process.directory(directory);

        process.environment().put("PATH", webpack.getParent() + ":" + process.environment().get("PATH"));

        // Add /usr/local/bin to the PATH in case of unixOS like
        if (IS_UNIX) {
            process.environment().put("PATH", process.environment().get("PATH") + ":/usr/local/bin");
        }

        process.command(new String[] { "node", webpack.getAbsolutePath(), "--config", webpackConfig.getAbsolutePath(),
                "--port", String.valueOf(port) });

        try {
            Process exec = process.start();
            Runtime.getRuntime().addShutdownHook(new Thread(exec::destroy));

            // Start a timer to avoid waiting for ever if pattern not found in webpack output.
            Thread timer = new Thread(() -> {
                try {
                    Thread.sleep(Integer.getInteger(PARAM_WEBPACK_TIMEOUT,
                            Integer.getInteger(PARAM_WEBPACK_TIMEOUT, DEFAULT_TIMEOUT_FOR_PATTERN)));
                    synchronized (this) {
                        notify(); //NOSONAR
                    }
                } catch (InterruptedException ignore) { //NOSONAR
                    getLogger().trace("Webpack timer interrupted");
                }
            });
            timer.start();

            logStream(exec.getErrorStream(), null);
            logStream(exec.getInputStream(), OUTPUT_PATTERN);

            synchronized (this) {
                this.wait();//NOSONAR
            }

            if (!exec.isAlive()) {
                throw new IllegalStateException("Webpack exited prematurely");
            }

            if (timer.isAlive()) {
                timer.interrupt();
            }
        } catch (IOException | InterruptedException e) {
            getLogger().error(e.getMessage(), e);
        }

        System.setProperty(PARAM_WEBPACK_RUNNING, String.valueOf(port));
    }
    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param configuration
     *         deployment configuration
     */
    public static void start(DeploymentConfiguration configuration) {
        devmodeHandler.compareAndSet(null,
                DevModeHandler.createInstance(configuration));
    }

    /**
     * Get the instantiated DevModeHandler.
     *
     * @return devModeHandler or {@code null} if not started
     */
    public static DevModeHandler getDevModeHandler() {
        return devmodeHandler.get();
    }

    /**
     * Constructs a dev mode server in the case that production mode or bower
     * mode are not set, and `webpack-dev-server` has been installed and
     * configured.
     *
     * @param configuration
     *            deployment configuration
     * @return the instance in case everything is alright, null otherwise
     */
    public static DevModeHandler createInstance(DeploymentConfiguration configuration) {

        if (configuration.isBowerMode() || configuration.isProductionMode()) {
            getLogger().trace("Instance not created because not in npm-dev mode");
            return null;
        }

        File directory = new File(WEBAPP_FOLDER).getAbsoluteFile();
        if (!directory.exists()) {
            getLogger().warn("Instance not created because cannot change to '{}'", directory);
            return null;
        }

        File webpack = new File(WEBPACK_SERVER);
        if (!webpack.canExecute()) {
            getLogger().warn("Instance not created because cannot execute '{}'. Did you run `npm install`", webpack);
            return null;
        } else if(!webpack.exists()) {
            getLogger().warn("Instance not created because file '{}' doesn't exist. Did you run `npm install`",
                    webpack);
            return null;
        }

        File webpackConfig = new File(WEBPACK_CONFIG);
        if (!webpackConfig.canRead()) {
            getLogger().warn("Instance not created because there is not webpack configuration '{}'", webpackConfig);
            return null;
        }

        return new DevModeHandler(directory, webpack, webpackConfig);
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
            connection.setRequestProperty(header, request.getHeader(header));
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

    // mirrors a stream to logger, and check whether a pattern is found in the output.
    private void logStream(InputStream input, Pattern pattern) {
        boolean notify = pattern != null;

        Thread thread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            try {
                for (String line; ((line = reader.readLine()) != null);) {
                    getLogger().info(line);
                    // We found the started pattern in stream, notify
                    // DevModeHandler to continue
                    if (notify && pattern.matcher(line).find()) {
                        synchronized (this) {
                            notify();//NOSONAR
                        }
                    }
                }
            } catch (IOException e) {
                getLogger().error("Exception when reading webpack output.", e);
            }

            // Process closed stream, means that it exited, notify
            // DevModeHandler to continue
            if (notify) {
                synchronized (this) {
                    notify();//NOSONAR
                }
            }
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
        return LoggerFactory.getLogger("c.v.f.s." + DevModeHandler.class.getSimpleName());
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
