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
package com.vaadin.flow.server.webjar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ResponseWriter;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Handles requests that may require webJars contents. In this case, writes the
 * required resource contents from a webJar into the response.
 * <p>
 * By default, webJars are enabled for development mode and disabled for
 * production mode. There is a way to override this behavior by setting
 * {@link Constants#DISABLE_WEBJARS} param.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class WebJarServer implements Serializable {
    private final ResponseWriter responseWriter;

    private final String prefix;
    private final Pattern urlPattern;

    /**
     * Creates a webJar server that is able to search webJars for files and
     * return them.
     *
     * @param deploymentConfiguration
     *            configuration for the deployment, not <code>null</code>
     *
     */
    public WebJarServer(DeploymentConfiguration deploymentConfiguration) {
        assert deploymentConfiguration != null;

        responseWriter = new ResponseWriter(deploymentConfiguration);

        String frontendPrefix = deploymentConfiguration
                .getDevelopmentFrontendPrefix();
        if (!frontendPrefix.endsWith("/")) {
            throw new IllegalArgumentException(
                    "Frontend prefix must end with a /. Got \"" + frontendPrefix
                            + "\"");
        }
        if (!frontendPrefix
                .startsWith(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)) {
            throw new IllegalArgumentException(
                    "Cannot host WebJars for a fronted prefix that isn't relative to 'context://'. Current frontend prefix: "
                            + frontendPrefix);
        }

        prefix = "/"
                + frontendPrefix.substring(
                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX.length())
                + "bower_components/";
        urlPattern = Pattern.compile("^([/.]?[/..]*)" + prefix);
    }

    /**
     * Searches for file requested in the webJars. If found, the file contents
     * is written into request.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return {@code true} if response was populated with webJar contents,
     *         {@code false} otherwise
     * @throws IOException
     *             if response population fails
     */
    public boolean tryServeWebJarResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String pathInContext = request.getPathInfo() == null
                ? request.getServletPath()
                : request.getServletPath() + request.getPathInfo();

        String webJarPath = getWebJarPath(pathInContext);
        if (webJarPath == null) {
            // Not a request to the frontend folder
            return false;
        }

        URL resourceUrl = request.getServletContext().getResource(webJarPath);
        if (resourceUrl == null) {
            // A request to the frontend folder but the file is not there,
            // 404 instead of forwarding to the router
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return true;
        }

        responseWriter.writeResponseContents(webJarPath, resourceUrl, request,
                response);
        return true;
    }

    /**
     * Gets web jar resource path if it exists.
     *
     * @param filePathInContext
     *            servlet context path for file
     * @return an optional web jar resource path, or an empty optional if the
     *         resource is not web jar resource
     */
    public Optional<String> getWebJarResourcePath(String filePathInContext) {
        String webJarPath = null;

        Matcher matcher = urlPattern.matcher(filePathInContext);
        // If we don't find anything then we don't have the prefix at all.
        if (matcher.find()) {
            webJarPath = getWebJarPath(
                    filePathInContext.substring(matcher.group(1).length()));
        }
        return Optional.ofNullable(webJarPath);
    }

    private String getWebJarPath(String path) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        return path.replace(prefix, "/webjars/");
    }
}
