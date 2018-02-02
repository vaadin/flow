/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.webjars.WebJarAssetLocator;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ResponseWriter;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.Json;

/**
 * Handles requests that may require webJars contents. In this case, writes the
 * required resource contents from a webJar into the response.
 * <p>
 * By default, webJars are enabled for development mode and disabled for
 * production mode. There is a way to override this behavior by setting
 * {@link Constants#DISABLE_WEBJARS} param.
 *
 * @author Vaadin Ltd.
 */
public class WebJarServer implements Serializable {
    private final transient WebJarAssetLocator locator = new WebJarAssetLocator();
    private final transient Map<String, WebJarBowerDependency> bowerModuleToDependencyName = new HashMap<>();
    private final ResponseWriter responseWriter = new ResponseWriter();

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

        locator.getWebJars().forEach((webJarName, version) -> {
            String bowerModuleName = getBowerModuleName(webJarName);
            if (bowerModuleName != null) {
                WebJarBowerDependency newDependency = new WebJarBowerDependency(
                        bowerModuleName, webJarName, version);
                bowerModuleToDependencyName.merge(bowerModuleName,
                        newDependency, this::mergeDependencies);
            }
        });
    }

    private WebJarBowerDependency mergeDependencies(
            WebJarBowerDependency oldDependency,
            WebJarBowerDependency newDependency) {
        int comparison = oldDependency.compareVersions(newDependency);
        if (comparison == 0) {
            LoggerFactory.getLogger(getClass().getName()).trace(
                    "Have found multiple webJars with name and version: '{}'",
                    oldDependency);
            return oldDependency;
        } else if (comparison > 0) {
            return oldDependency;
        } else {
            return newDependency;
        }
    }

    private String getBowerModuleName(String webJarName) {
        String bowerJsonPath = locator.getFullPathExact(webJarName,
                "bower.json");
        if (bowerJsonPath == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(bowerJsonPath),
                StandardCharsets.UTF_8))) {
            return Json.parse(reader.lines().collect(Collectors.joining()))
                    .getString("name");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read resource located at: " + bowerJsonPath, e);
        }
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
        String pathInContext = request.getServletPath() + request.getPathInfo();

        String webJarPath = getWebJarPath(pathInContext);
        if (webJarPath == null) {
            return false;
        }

        URL resourceUrl = request.getServletContext().getResource(webJarPath);
        if (resourceUrl == null) {
            return false;
        }

        responseWriter.writeResponseContents(webJarPath, resourceUrl, request,
                response);
        return true;
    }

    /**
     * Check if a file is existent in a WebJar.
     * 
     * @param filePathInContext
     *            servlet context path for file
     * @param servletContext
     *            servlet context
     * @return true if file is found else false
     * @throws IOException
     *             if response population fails
     */
    public boolean hasWebJarResource(String filePathInContext,
            ServletContext servletContext) throws IOException {
        String webJarPath = null;

        Matcher matcher = urlPattern.matcher(filePathInContext);
        // If we don't find anything then we don't have the prefix at all.
        if (matcher.find()) {
            webJarPath = getWebJarPath(
                    filePathInContext.substring(matcher.group(1).length()));
        }
        if (webJarPath == null) {
            return false;
        }

        return servletContext.getResource(webJarPath) != null;
    }

    private String getWebJarPath(String path) {
        if (bowerModuleToDependencyName.isEmpty() || !path.startsWith(prefix)) {
            return null;
        }
        String pathWithoutPrefix = path.substring(prefix.length());

        int separatorIndex = pathWithoutPrefix.indexOf('/');
        if (separatorIndex < 0) {
            return null;
        }

        String bowerModuleName = pathWithoutPrefix.substring(0, separatorIndex);
        WebJarBowerDependency dependency = bowerModuleToDependencyName
                .get(bowerModuleName);
        if (dependency == null) {
            return null;
        }

        return String.join("", "/webjars/", dependency.toWebPath(),
                pathWithoutPrefix.substring(separatorIndex));
    }
}
