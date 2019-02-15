/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

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
public class DevModeServer implements Serializable {

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private static final int DEFAULT_TIMEOUT = 60 * 1000;
    private static final Integer WEBPACK_PORT = 8081;
    private static final String WEBPACK_HOST = "http://localhost";

    static final String WEBAPP_FOLDER = "src/main/webapp/";
    static final String WEBPACK_CONFIG = "webpack.config.js";
    static final String WEBPACK_SERVER = "node_modules/.bin/webpack-dev-server";

    private static Process exec;

    // For testing purposes
    DevModeServer() {
    }

    private DevModeServer(File directory, File webpack, File webpackConfig) {
        if (exec != null && exec.isAlive()) {
            getLogger().info("Webpack is already running.");
            return;
        }

        getLogger().info("Starting Webpack in dev mode ...");
        ProcessBuilder process = new ProcessBuilder();
        process.directory(directory);

        process.environment().put("PATH", webpack.getParent() + ":" + process.environment().get("PATH"));

        // Add /usr/local/bin to the PATH in case of unixOS like
        File shell = new File("/bin/sh");
        if (shell.canExecute()) {
            process.environment().put("PATH", process.environment().get("PATH") + ":/usr/local/bin");
        }

        process.command(new String[] {
            webpack.getAbsolutePath(),
            "--config", webpackConfig.getAbsolutePath(),
            "--port", WEBPACK_PORT.toString()
        });

        try {
            exec = process.start();

            Runtime.getRuntime().addShutdownHook(new Thread(exec::destroy));
            logStream(exec.getInputStream());
            logStream(exec.getErrorStream());

            // webpack takes a while to listen
            Thread.sleep(2000);
        } catch (IOException e) {
            getLogger().error(e.getMessage(), e);
        } catch (InterruptedException e) {
            getLogger().trace(e.getMessage(), e);
        }
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
    public static DevModeServer createInstance(DeploymentConfiguration configuration) {
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
        }

        File webpackConfig = new File(WEBPACK_CONFIG);
        if (!webpackConfig.canRead()) {
            getLogger()
                    .warn("Instance not created because there is not webpack configuration '{}'", webpackConfig);
            return null;
        }

        return new DevModeServer(directory, webpack, webpackConfig);
    }    

    /**
     * Returns true if it's a request that should be handled by webpack.
     *
     * @param request
     * @return true if the request should be forwarded to webpack
     */
    public boolean isDevModeRequest(HttpServletRequest request) {
        return getRequestFilename(request).matches(".+\\.js");
    }

    /**
     * Serve a file by proxying to webpack.
     *
     * @param request
     * @param response
     * @return false if webpack returned a not found, true otherwise
     * @throws IOException
     */
    public boolean serveDevModeRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestFilename = getRequestFilename(request);

        URL uri = new URL(WEBPACK_HOST + ":" + WEBPACK_PORT + requestFilename);

        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod(request.getMethod());
        connection.setReadTimeout(DEFAULT_TIMEOUT);
        connection.setConnectTimeout(DEFAULT_TIMEOUT);

        // Copies all the headers from the original request
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            connection.setRequestProperty(header, request.getHeader(header));
        }

        // Send the request
        if (connection.getResponseCode() == 404) {
            // webpack cannot access the resource, return false so as flow can
            // handle it
            return false;
        }

        getLogger().trace("Served resource by webpack: {}", uri);

        // Copies response headers
        connection.getHeaderFields().forEach((header, values) -> {
            if (header != null) {
                response.addHeader(header, values.get(0));
            }
        });
        // Copies response payload
        writeStream(response.getOutputStream(), connection.getInputStream());
        // Copies response code
        response.sendError(connection.getResponseCode());

        return true;
    }
    
    private void logStream(InputStream input) {
        Thread t = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            try {
                for (String line; ((line = reader.readLine()) != null);) {
                    getLogger().info(line);
                }
            } catch (IOException e) {
            }
        });
        t.setName("webpack");
        t.start();
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
        return LoggerFactory.getLogger("c.v.f.s." + DevModeServer.class.getSimpleName());
    }
}
